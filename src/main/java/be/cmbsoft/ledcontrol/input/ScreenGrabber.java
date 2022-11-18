package be.cmbsoft.ledcontrol.input;

import be.cmbsoft.ledcontrol.LedController;
import com.sun.jna.platform.DesktopWindow;
import com.sun.jna.platform.WindowUtils;
import com.sun.jna.platform.win32.WinDef;
import processing.core.PGraphics;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class ScreenGrabber implements Input {

    private final Robot robot;
    private final WinDef.HWND handle;
    private PGraphics buffer;

    public ScreenGrabber() {
        try {
            robot = new Robot();
            List<DesktopWindow> allWindows = WindowUtils.getAllWindows(true);
            DesktopWindow projectMWindow = allWindows.stream()
                    .filter(window -> window.getTitle().contains("EyeTune") || window.getTitle().contains("projectM"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("ProjectM does not appear to be launched?"));
            handle = projectMWindow.getHWND();

        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void drawGraphics(PGraphics matrix, LedController ledController) {
        Rectangle locAndSize = WindowUtils.getWindowLocationAndSize(handle);
        // Reduce rectangle size to avoid capturing window borders
        locAndSize.setSize(locAndSize.width - 10, locAndSize.height - 30);
        locAndSize.setLocation(locAndSize.x + 5, locAndSize.y + 25);
        BufferedImage screenCapture = robot.createScreenCapture(locAndSize);
        if (buffer == null || buffer.width != screenCapture.getWidth() || buffer.height != screenCapture.getHeight()) {
            buffer = ledController.createGraphics(screenCapture.getWidth(), screenCapture.getHeight());
        }
        buffer.beginDraw();
        buffer.loadPixels();
        screenCapture.getRGB(0, 0, buffer.width, buffer.height, buffer.pixels, 0, buffer.width);
        buffer.updatePixels();
        buffer.endDraw();
        matrix.image(buffer, 0, 0, matrix.width, matrix.height);
    }
}
