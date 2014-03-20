package automatizer.core.utils;

import java.io.OutputStream;

import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.zest.core.widgets.Graph;

public class FileUtils {

    /**
     * 将当前 Zest 图像控件的内容另存为给定格式的图像
     * 
     * @param g
     * @param stream
     * @param format
     *            可以是如下常量之一：
     *            <ul>
     *            <li>SWT.IMAGE_BMP</li>
     *            <li>SWT.IMAGE_BMP_RLE</li>
     *            <li>SWT.IMAGE_GIF</li>
     *            <li>SWT.IMAGE_ICO</li>
     *            <li>SWT.IMAGE_JPEG</li>
     *            <li>SWT.IMAGE_PNG</li>
     *            </ul>
     */
    public static void saveImageToStream(Graph g, OutputStream stream,
            int format) {
        Rectangle bounds = g.getContents().getBounds();
        Dimension dim = g.getContents().getSize();
        Point viewLocation = g.getViewport().getViewLocation();

        final Image image = new Image(null, dim.width(), dim.height());
        GC gc = new GC(image);
        SWTGraphics swtGraphics = new SWTGraphics(gc);
        swtGraphics.translate(-1 * bounds.x + viewLocation.x, -1 * bounds.y
                + viewLocation.y);
        g.getViewport().paint(swtGraphics);
        gc.copyArea(image, 0, 0);
        gc.dispose();

        ImageLoader loader = new ImageLoader();
        loader.data = new ImageData[] { image.getImageData() };
        loader.save(stream, format);
    }

}
