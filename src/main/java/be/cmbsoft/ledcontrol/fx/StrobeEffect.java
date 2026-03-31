package be.cmbsoft.ledcontrol.fx;

import processing.core.PApplet;
import processing.core.PGraphics;

public class StrobeEffect implements Effect {

    float frequency;
    long lastTime;
    long inverseFrequency;
    boolean on = false;

    public StrobeEffect() {
        setFrequency(10);
    }

    @Override
    public String getName() {
        return "strobe";
    }

    @Override
    public void apply(PGraphics overlay, PApplet applet) {
        overlay.beginDraw();
        if (applet.millis() - lastTime > inverseFrequency) {
            lastTime = applet.millis();
            on = !on;
        }
        if (on) {
            overlay.background(0, 255);
        } else {
            overlay.background(0);
        }
        overlay.endDraw();
    }

    public void setFrequency(float frequency) {
        this.frequency = frequency;
        this.inverseFrequency = (long) (1000 / frequency);
    }

}
