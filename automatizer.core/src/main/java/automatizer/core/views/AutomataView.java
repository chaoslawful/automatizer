package automatizer.core.views;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.HorizontalTreeLayoutAlgorithm;

import automatizer.core.AutomatizerActivator;
import automatizer.core.adapters.AutomataGraphContentProvider;
import automatizer.core.adapters.AutomataLabelProvider;
import automatizer.core.utils.FileUtils;
import automatizer.core.utils.GraphvizUtils;
import automatizer.core.utils.Logger;

import com.etao.lz.automaton.Automaton;
import com.etao.lz.automaton.State;
import com.etao.lz.automaton.Transition;
import com.etao.lz.recollection.JavaRegExp;
import com.etao.lz.recollection.ast.Converter;
import com.etao.lz.recollection.utils.AutomatonTool;

/**
 * 以可视化方式显示状态机转移图
 * 
 * TODO 整合某种 Regexp minimization 算法（不可能是高效算法，因该问题是 PSPACE-complete
 * 的），并实现简化选中正则表达式的功能
 * 
 * TODO 整合某种 NFA minimization 算法（不可能是高效算法，因该问题是 PSPACE-complete 的），并实现显示 min NFA
 * 的功能
 * 
 * TODO 更改 Automaton 的 transition 实现使其支持 ε-transition，并实现显示 ε-NFA 的功能
 * 
 * LATER 覆盖 Zest 默认的 ChopBoxAnchor 锚点计算方法，让转移边能连接到状态圈上
 * 
 * LATER 使用其他布局算法替代 Zest 内置算法，希望能达到接近 Graphviz 布局的效果
 * 
 * LATER 更新 StateFigure 使其能像内置形状一样可以通过 LabelProvider 的 getText()
 * 方法提供标签，而非像现在这样在生成 Figure 对象时绑定标签（Zest 1.x 的自定义形状 bug，暂无法解决，等 Zest 2.x 成熟后再说）
 * 
 * DONE 更新 StateFigure 使其支持 initial state 形状
 * 
 * DONE 支持将转移图另存为 DOT 文件
 * 
 * DONE 支持解析并显示用 DOT 语法描述的状态机
 * 
 * DONE 支持将 DOT 语法描述的状态机转换为 regexp（通过 state elimination/removal 算法）
 * 
 * @author wxz
 * 
 */
public class AutomataView extends ViewPart {

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "automatizer.core.views.AutomataView";

    // 用于识别 Graphviz DOT 格式文本的正则模式
    private static final Pattern GRAPHVIZ_PATTERN = Pattern
            .compile("^\\s*digraph\\s+\\w+\\s*\\{");

    private Action actionStreaming; // 切换流式/非流式匹配状态机
    private Action actionShowMinNFA; // 切换至 ε-free NFA 显示方式
    private Action actionShowMinDFA; // 切换至最小化 DFA 显示方式
    private Action actionExportAsImage; // 保存成图片
    private Action actionExportAsDot; // 保存成 Graphviz DOT 文件
    private Action actionToggleRegexp; // 显示从状态机转换出的正则表达式

    private ISelectionListener pageSelectionListener;

    enum AutomataType {
        EPS_FREE_NFA, MIN_DFA
    };

    private boolean streamingMode; // 是否转换为可流式匹配的状态机
    private boolean showRegexp; // 是否同步显示状态机对应的正则表达式
    private AutomataType automataType; // 转换目标状态机类型
    private GraphViewer viewer; // Zest 自动布局图展示控件
    private Text regexArea; // 显示当前状态机对应的正则表达式
    private String curSelTxt; // 当前选中的文本

    public AutomataView() {
        streamingMode = false;
        showRegexp = false;
        automataType = AutomataType.EPS_FREE_NFA;
    }

    /**
     * 视图创建时初始化回调函数
     */
    @Override
    public void createPartControl(Composite parent) {
        createDiagram(parent);

        makeActions();
        hookContextMenu();
        contributeToActionBars();
        hookPageSelection();
    }

    @Override
    public void setFocus() {
    }

    @Override
    public void dispose() {
        setModel(null);

        // 销毁视图时移除之前注册的文本选中事件处理逻辑
        if (pageSelectionListener != null) {
            getSite().getPage().removePostSelectionListener(
                    pageSelectionListener);
        }

        super.dispose();
    }

    public void refreshDiagram() {
        // 刷新当前状态机转移图
        Automaton atm = txtToAutomaton(curSelTxt);
        setModel(atm);

        // 刷新当前状态机对应的正则表达式
        if (showRegexp) {
            String regex = AutomatonTool.aToRe(atm);
            regexArea.setText(regex);
        } else {
            regexArea.setText("");
        }
    }

    private Automaton txtToAutomaton(String txt) {
        Automaton atm = null;
        boolean minimize = false;

        if (automataType == AutomataType.MIN_DFA) {
            minimize = true;
        }

        if (txt != null && !txt.isEmpty()) {
            Matcher matcher = GRAPHVIZ_PATTERN.matcher(txt);
            if (matcher.find()) {
                // 选中文本作为 Graphviz DOT 处理
                try {
                    atm = GraphvizUtils.dotToFsa(new StringBuffer(txt));
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                // 选中文本作为正则表达式处理
                JavaRegExp jre = new JavaRegExp(txt);
                atm = jre.toAutomaton(false);
            }

            atm = Converter.transformAutomaton(atm, minimize, streamingMode);
            setStateNumbers(atm);
        }

        return atm;
    }

    private void setModel(Automaton newAutomata) {
        if (viewer != null
                && (viewer.getControl() != null && !viewer.getControl()
                        .isDisposed())) {
            viewer.setInput(newAutomata);
        }
    }

    private void createDiagram(Composite parent) {
        parent.setLayout(new GridLayout(1, true));

        regexArea = new Text(parent, SWT.SINGLE | SWT.READ_ONLY);
        regexArea.setText("");
        regexArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        viewer = new GraphViewer(parent, SWT.NONE);
        viewer.setContentProvider(new AutomataGraphContentProvider());
        viewer.setLabelProvider(new AutomataLabelProvider());
        int styles = LayoutStyles.NO_LAYOUT_NODE_RESIZING;
        viewer.setLayoutAlgorithm(new HorizontalTreeLayoutAlgorithm(styles));

        Control ctrl = viewer.getControl();
        ctrl.setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    /**
     * 增加文本选中事件处理逻辑
     */
    private void hookPageSelection() {
        pageSelectionListener = new ISelectionListener() {
            @Override
            public void selectionChanged(IWorkbenchPart part,
                    ISelection selection) {
                pageSelectionChanged(part, selection);
            }
        };
        getSite().getPage().addPostSelectionListener(pageSelectionListener);

        // 用当前选中内容初始化状态图
        ISelectionService selectionService = getSite().getWorkbenchWindow()
                .getSelectionService();
        updateCurText(selectionService.getSelection());
        refreshDiagram();
    }

    /**
     * 提取选中区域文本并触发状态机转换图更新
     * 
     * @param part
     * @param selection
     */
    private void pageSelectionChanged(IWorkbenchPart part, ISelection selection) {
        if (part == this) {
            return;
        }
        updateCurText(selection);
        refreshDiagram();
    }

    private void updateCurText(ISelection selection) {
        if (!(selection instanceof TextSelection)) {
            return;
        }

        TextSelection txtSel = (TextSelection) selection;
        curSelTxt = txtSel.getText();
    }

    /**
     * 从起始状态开始逐级给自动机状态重新编号，以便展现美观
     * 
     * @param atm
     */
    private final void setStateNumbers(Automaton atm) {
        int num = 0;
        Set<State> accSet = new HashSet<State>();
        List<State> workQ = new ArrayList<State>();
        State initState = atm.getInitialState();
        workQ.add(initState);
        accSet.add(initState);
        while (!workQ.isEmpty()) {
            State state = workQ.remove(0);
            state.setNumber(num++);
            for (Transition tran : state.getSortedTransitions(false)) {
                State dst = tran.getDest();
                if (!accSet.contains(dst)) {
                    workQ.add(dst);
                    accSet.add(dst);
                }
            }
        }
    }

    /**
     * 修改视图右键快捷菜单
     */
    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                // 向视图右键快捷菜单增加新条目
                addAutomatizerMenuItems(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, viewer);
    }

    /**
     * 修改视图右上方工具栏
     */
    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();

        // 向视图右上角下拉菜单增加新项目
        addAutomatizerMenuItems(bars.getMenuManager());

        // 向视图右上方工具栏增加新按钮
        // IToolBarManager tbManager = bars.getToolBarManager();
        // tbManager.add(actionStepping);

        bars.updateActionBars();
    }

    private void addAutomatizerMenuItems(IMenuManager manager) {
        manager.add(actionStreaming);
        manager.add(actionShowMinNFA);
        manager.add(actionShowMinDFA);
        manager.add(actionToggleRegexp);
        manager.add(actionExportAsImage);
        manager.add(actionExportAsDot);
        // 允许其他插件在此之后继续添加新条目
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    private void makeActions() {
        ImageDescriptor automatizerIconDesc = AutomatizerActivator
                .getImageDescriptor("icons/automatizer-16.png");

        actionExportAsDot = new Action() {
            @Override
            public void run() {
                exportGraphToDot();
            }
        };
        actionExportAsDot.setText("Export as DOT");
        actionExportAsDot.setToolTipText("Save current graph as Graphviz DOT");
        actionExportAsDot.setImageDescriptor(automatizerIconDesc);

        actionExportAsImage = new Action() {
            @Override
            public void run() {
                exportGraphToImage();
            }
        };
        actionExportAsImage.setText("Export as Image");
        actionExportAsImage.setToolTipText("Save current graph as image");
        actionExportAsImage.setImageDescriptor(automatizerIconDesc);

        actionStreaming = new Action() {
            @Override
            public void run() {
                streamingMode = !streamingMode;
                refreshDiagram();
            }
        };
        actionStreaming.setText("Toggle streaming match");
        actionStreaming
                .setToolTipText("Toggle streaming/non-streaming matching FSA");
        actionStreaming.setImageDescriptor(PlatformUI.getWorkbench()
                .getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

        actionShowMinNFA = new Action() {
            @Override
            public void run() {
                automataType = AutomataType.EPS_FREE_NFA;
                refreshDiagram();
            }
        };
        actionShowMinNFA.setText("Show ε-free NFA");
        actionShowMinNFA
                .setToolTipText("Show simplified NFA without ε-transitions");
        actionShowMinNFA.setImageDescriptor(PlatformUI.getWorkbench()
                .getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

        actionShowMinDFA = new Action() {
            @Override
            public void run() {
                automataType = AutomataType.MIN_DFA;
                refreshDiagram();
            }
        };
        actionShowMinDFA.setText("Show min DFA");
        actionShowMinDFA.setToolTipText("Show minimized DFA");
        actionShowMinDFA.setImageDescriptor(PlatformUI.getWorkbench()
                .getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

        actionToggleRegexp = new Action() {
            @Override
            public void run() {
                showRegexp = !showRegexp;
                if (showRegexp) {
                    actionToggleRegexp.setText("Hide Regexp");
                    actionToggleRegexp
                            .setToolTipText("Hide regexp corresponding to current FSA");
                } else {
                    actionToggleRegexp.setText("Show Regexp");
                    actionToggleRegexp
                            .setToolTipText("Show regexp corresponding to current FSA");
                }
                refreshDiagram();
            }
        };
        actionToggleRegexp.setText("Show Regexp");
        actionToggleRegexp
                .setToolTipText("Show regexp corresponding to current FSA");
        actionToggleRegexp.setImageDescriptor(PlatformUI.getWorkbench()
                .getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
    }

    private void exportGraphToImage() {
        FileDialog dialog = new FileDialog(getViewSite().getShell(), SWT.SAVE);
        String[] filterNames = new String[] { "PNG Files (*.png)",
                "JPG Files (*.jpg)", "GIF Files (*.gif)", "BMP Files (*.bmp)",
                "All Files (*)" };
        String[] filterExtensions = new String[] { "*.png", "*.jpg", "*.gif",
                "*.bmp", "*" };
        dialog.setFilterNames(filterNames);
        dialog.setFilterExtensions(filterExtensions);
        dialog.setFileName("automata");
        String filePath = dialog.open();
        if (filePath != null) {
            try {
                int format = SWT.IMAGE_PNG;
                String lowerFilePath = filePath.toLowerCase();
                if (lowerFilePath.endsWith("gif")) {
                    format = SWT.IMAGE_GIF;
                } else if (lowerFilePath.endsWith("png")) {
                    format = SWT.IMAGE_PNG;
                } else if (lowerFilePath.endsWith("bmp")) {
                    format = SWT.IMAGE_BMP_RLE;
                } else if (lowerFilePath.endsWith("jpg")) {
                    format = SWT.IMAGE_JPEG;
                }
                Graph g = (Graph) viewer.getControl();
                FileOutputStream fos = new FileOutputStream(filePath);
                FileUtils.saveImageToStream(g, fos, format);
                fos.close();
            } catch (Exception e) {
                Logger.err(e.getLocalizedMessage());
            }
        }
    }

    private void exportGraphToDot() {
        Automaton atm = (Automaton) viewer.getInput();
        if (atm == null) {
            return;
        }

        FileDialog dialog = new FileDialog(getViewSite().getShell(), SWT.SAVE);
        String[] filterNames = new String[] { "DOT Files (*.dot)",
                "All Files (*)" };
        String[] filterExtensions = new String[] { "*.dot", "*" };
        dialog.setFilterNames(filterNames);
        dialog.setFilterExtensions(filterExtensions);
        dialog.setFileName("automata");
        String filePath = dialog.open();
        if (filePath != null) {
            try {
                FileOutputStream fos = new FileOutputStream(filePath);
                String dot = atm.toDot();
                fos.write(dot.getBytes("UTF-8"));
                fos.close();
            } catch (Exception e) {
                Logger.err(e.getLocalizedMessage());
            }
        }
    }

}
