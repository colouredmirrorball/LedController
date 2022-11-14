package be.cmbsoft.ledcontrol;

import com.illposed.osc.MessageSelector;
import com.illposed.osc.OSCMessageEvent;
import com.illposed.osc.OSCMessageListener;
import com.illposed.osc.transport.OSCPortIn;
import com.illposed.osc.transport.OSCPortInBuilder;
import processing.core.PApplet;

import java.io.IOException;

public class LedController extends PApplet implements OSCMessageListener {

    private final OSCPortIn port;

    public LedController() {
        MessageSelector selector = new MessageSelector() {
            @Override
            public boolean isInfoRequired() {
                return false;
            }

            @Override
            public boolean matches(OSCMessageEvent messageEvent) {
                return true;
            }
        };
        try {
            port = new OSCPortInBuilder()
                    .setLocalPort(5014)
                    .addMessageListener(selector, this)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        runSketch(new String[]{"be.cmbsoft.ledcontrol.LedController"}, new LedController());
    }

    @Override
    public void settings() {
        size(1200, 800);
    }

    @Override
    public void setup() {
        background(0);
    }

    @Override
    public void draw() {

    }

    @Override
    public void acceptMessage(OSCMessageEvent oscMessageEvent) {
        System.out.println(oscMessageEvent.getMessage());
    }
}
