package be.cmbsoft.ledcontrol.fx;

import processing.core.PApplet;
import processing.core.PGraphics;

public interface Effect {

    void apply(PGraphics overlay, PApplet applet);

    String getName();

}
