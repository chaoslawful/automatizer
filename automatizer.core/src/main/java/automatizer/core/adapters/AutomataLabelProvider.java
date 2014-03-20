package automatizer.core.adapters;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.zest.core.viewers.IEntityConnectionStyleProvider;
import org.eclipse.zest.core.viewers.IFigureProvider;
import org.eclipse.zest.core.widgets.ZestStyles;

import automatizer.core.figures.StateFigure;
import automatizer.core.models.ConnectionData;
import automatizer.core.models.StateData;

/**
 * 供 Zest 使用的将状态机 model 转换为展现信息的适配器
 * 
 * @author wxz
 * 
 */
public class AutomataLabelProvider implements ILabelProvider, IColorProvider,
        IEntityConnectionStyleProvider, IFigureProvider {

    @Override
    public void addListener(ILabelProviderListener listener) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
    }

    @Override
    public Image getImage(Object element) {
        return null;
    }

    @Override
    public String getText(Object element) {
        if (element instanceof StateData) {
            StateData state = (StateData) element;
            return state.getLabel();
        }
        if (element instanceof ConnectionData) {
            ConnectionData conn = (ConnectionData) element;
            return conn.getLabel();
        }
        return null;
    }

    @Override
    public Color getForeground(Object element) {
        return ColorConstants.black;
    }

    @Override
    public Color getBackground(Object element) {
        if (element instanceof StateData) {
            StateData state = (StateData) element;
            if (state.isAccept()) {
                return ColorConstants.lightGray;
            }
        }
        return ColorConstants.white;
    }

    @Override
    public int getConnectionStyle(Object src, Object dest) {
        return ZestStyles.CONNECTIONS_DIRECTED;
    }

    @Override
    public Color getColor(Object src, Object dest) {
        return ColorConstants.black;
    }

    @Override
    public Color getHighlightColor(Object src, Object dest) {
        return ColorConstants.lightGreen;
    }

    @Override
    public int getLineWidth(Object src, Object dest) {
        return -1;
    }

    @Override
    public IFigure getTooltip(Object entity) {
        return null;
    }

    @Override
    public IFigure getFigure(Object element) {
        if (element instanceof StateData) {
            StateData state = (StateData) element;
            StateFigure f = new StateFigure(state.getLabel(),
                    state.isInit(), state.isAccept());
            f.setSize(f.getPreferredSize());
            return f;
        }
        return null;
    }

}
