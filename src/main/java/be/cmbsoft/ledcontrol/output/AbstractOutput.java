package be.cmbsoft.ledcontrol.output;

import be.cmbsoft.ledcontrol.LedController;

public abstract class AbstractOutput {
    public abstract void send(LedController.PixelFetcher getData);

    public abstract void stop();
}
