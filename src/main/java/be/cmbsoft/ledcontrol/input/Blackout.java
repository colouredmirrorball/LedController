package be.cmbsoft.ledcontrol.input;

import be.cmbsoft.ledcontrol.LedController;
import processing.core.PGraphics;

public class Blackout implements Input {
    @Override
    public void drawGraphics(PGraphics matrix, LedController ledController) {
        matrix.beginDraw();
        matrix.background(0);
        matrix.endDraw();
    }
}
