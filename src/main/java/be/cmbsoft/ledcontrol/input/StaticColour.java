package be.cmbsoft.ledcontrol.input;

import be.cmbsoft.ledcontrol.LedController;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;

public class StaticColour implements Input {
    @Override
    public void drawGraphics(PGraphics matrix, LedController ledController) {
        matrix.beginDraw();
        ledController.colorMode(PConstants.HSB);
        matrix.background(
                ledController.color(PApplet.map(ledController.mouseX, 0, ledController.width, 0, 255),
                        PApplet.map(ledController.mouseY, 0, ledController.height, 0, 255), 255));
        matrix.endDraw();
    }
}
