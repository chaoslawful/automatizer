package automatizer.core.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

import automatizer.core.utils.Logger;
import automatizer.core.views.AutomataView;

/**
 * 显示自动机视图命令处理器
 * 
 * @author wxz
 * 
 */
public class ShowAutomataViewHandler extends AbstractHandler {

    public ShowAutomataViewHandler() {
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil
                .getActiveWorkbenchWindowChecked(event);
        if (window == null) {
            return null;
        }

        IWorkbenchPage page = window.getActivePage();
        if (page == null) {
            return null;
        }

        try {
            page.showView(AutomataView.ID);
        } catch (PartInitException e) {
            Logger.err(e.getLocalizedMessage());
        }

        return null;
    }

}
