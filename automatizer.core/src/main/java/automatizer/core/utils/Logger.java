package automatizer.core.utils;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import automatizer.core.AutomatizerActivator;

/**
 * 插件日志方法
 * 
 * @author wxz
 * 
 */
public class Logger {

    private static ILog logger() {
        return AutomatizerActivator.getDefault().getLog();
    }

    public static void info(String msg) {
        logger().log(new Status(IStatus.INFO, AutomatizerActivator.PLUGIN_ID, msg));
    }

    public static void warn(String msg) {
        logger().log(new Status(IStatus.WARNING, AutomatizerActivator.PLUGIN_ID, msg));
    }

    public static void err(String msg) {
        logger().log(new Status(IStatus.ERROR, AutomatizerActivator.PLUGIN_ID, msg));
    }

}
