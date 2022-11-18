package be.cmbsoft.ledcontrol.input;

import be.cmbsoft.ledcontrol.LedController;
import processing.core.PGraphics;

public interface Input {
    void drawGraphics(PGraphics matrix, LedController ledController);
}
