package automatizer.core.figures;

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class StateFigure extends Shape {

    final static int ARROW_WIDTH = 8;
    final static int ARROW_HEIGHT = 8;
    final static int ARROW_LINE_LEN = 15;
    final static int GAP = 5;
    final static int SIZE = 30;

    boolean init;
    boolean accept;
    String text;

    public StateFigure(String label, boolean isInit, boolean isAccept) {
        text = label;
        init = isInit;
        accept = isAccept;

        int leftMargin = isInit ? ARROW_LINE_LEN : 0;
        Border border = new MarginBorder(0, leftMargin, 1, 1);
        setBorder(border);
        setPreferredSize(SIZE + leftMargin, SIZE);

        // 手工计算文本包围框太麻烦，直接用现成的控件
        setLayoutManager(new StackLayout());
        add(new Label(text));
    }

    @Override
    protected void outlineShape(Graphics graphics) {
        graphics.drawOval(getClientArea());

        if (init) {
            drawInitToken(graphics);
        }
        if (accept) {
            drawAcceptToken(graphics);
        }
        drawLabel(graphics);
    }

    @Override
    protected void fillShape(Graphics graphics) {
        graphics.fillOval(getClientArea());

        if (init) {
            drawInitToken(graphics);
        }
        if (accept) {
            drawAcceptToken(graphics);
        }
        drawLabel(graphics);
    }

    /**
     * 绘制状态文本内容
     * 
     * @param graphics
     */
    private void drawLabel(Graphics graphics) {
        // graphics.drawText(text, getClientArea().getCenter());
    }

    /**
     * 绘制 accept 状态内圆
     * 
     * @param graphics
     */
    private void drawAcceptToken(Graphics graphics) {
        Rectangle r = Rectangle.SINGLETON.setBounds(getClientArea());
        r.shrink(GAP, GAP);
        graphics.drawOval(r);
    }

    /**
     * 绘制水平方向指向 init 状态外圆的箭头
     * 
     * @param graphics
     */
    private void drawInitToken(Graphics graphics) {
        Rectangle clientArea = getClientArea();
        Point p1 = new Point(clientArea.x - ARROW_LINE_LEN,
                clientArea.getLeft().y);
        Point p2 = clientArea.getLeft();
        Point a1 = new Point(p2).translate(-ARROW_WIDTH, -ARROW_HEIGHT / 2);
        Point a2 = new Point(p2).translate(-ARROW_WIDTH, ARROW_HEIGHT / 2);

        // 绘制箭头
        graphics.drawLine(p1, p2);
        graphics.drawLine(p2, a1);
        graphics.drawLine(p2, a2);
    }

    public static void main(String[] args) {
        Display display = new Display();
        Shell shell = new Shell(display);
        shell.setSize(400, 400);
        shell.setText("test");
        shell.setLayout(new GridLayout());

        Figure root = new Figure();
        root.setFont(shell.getFont());
        XYLayout layout = new XYLayout();
        root.setLayoutManager(layout);

        StateFigure state = new StateFigure("0", false, false);
        state.setSize(state.getPreferredSize());
        root.add(state, new Rectangle(new Point(10, 10), state.getBounds()
                .getSize()));

        // RectangleFigure rect = new RectangleFigure();
        // rect.setSize(state.getBounds().getSize());
        // rect.setFill(false);
        // root.add(rect, new Rectangle(new Point(10, 10), rect.getBounds()
        // .getSize()));

        Canvas canvas = new Canvas(shell, SWT.DOUBLE_BUFFERED);
        canvas.setBackground(ColorConstants.white);
        canvas.setLayoutData(new GridData(GridData.FILL_BOTH));
        LightweightSystem lws = new LightweightSystem(canvas);
        lws.setContents(root);

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        display.dispose();
    }

}
