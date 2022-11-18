package be.cmbsoft.ledcontrol.input;

import be.cmbsoft.ledcontrol.LedController;
import com.sun.jna.platform.DesktopWindow;
import com.sun.jna.platform.WindowUtils;
import com.sun.jna.platform.win32.WinDef;
import processing.core.PGraphics;
import processing.core.PImage;

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
            List<DesktopWindow> allWindows = WindowUtils.getAllWindows(false);
            DesktopWindow projectMWindow = allWindows.stream()
                    .filter(window -> window.getTitle().contains("EyeTune") || window.getTitle().contains(
                            "projectM")).findFirst()
                    .orElseThrow(() -> new RuntimeException("ProjectM does not appear to be " +
                            "launched?"));
            handle = projectMWindow.getHWND();

        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void drawGraphics(PGraphics matrix, LedController ledController) {
        Rectangle locAndSize = WindowUtils.getWindowLocationAndSize(handle);
        BufferedImage screenCapture = robot.createScreenCapture(locAndSize);
//        if (buffer == null || buffer.width != locAndSize.width || buffer.height != locAndSize.height) {
//            buffer = ledController.createGraphics(locAndSize.width, locAndSize.height);
//        }
//        buffer.beginDraw();
//        buffer.loadPixels();
//        int index = 0;
//        for (int x = 0; x < screenCapture.getWidth(); x++) {
//            for (int y = 0; y < screenCapture.getHeight(); y++) {
//                buffer.pixels[index++] = screenCapture.getRGB(x, y);
//            }
//        }
//        buffer.updatePixels();
//        buffer.endDraw();
//        matrix.image(buffer, 0, 0, matrix.width, matrix.height);
        matrix.image(new PImage(screenCapture), 0, 0, matrix.width, matrix.height);
    }
}
