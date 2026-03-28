package be.cmbsoft.ledcontrol.console;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCSerializeException;
import com.illposed.osc.transport.OSCPortOut;
import com.illposed.osc.transport.OSCPortOutBuilder;
import processing.core.PApplet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

public class Console extends PApplet {

    private final String[] faderNames = new String[]{"Intensity"};
    private final String[] buttonNames = new String[]{"Off", "ProjectM"};
    private final Fader[] faders = new Fader[faderNames.length];
    private final ToggleButton[] buttons = new ToggleButton[buttonNames.length];


    private int activeFader = -1;
    private OSCPortOut oscOut;
    private String oscStatus = "Disconnected";

    private int prevWidth;
    private int prevHeight;

    public static void main(String[] args) {
        PApplet.main("be.cmbsoft.ledcontrol.console.Console");
    }

    @Override
    public void settings() {
        size(720, 360);
        smooth(4);
    }

    @Override
    public void setup() {
        surface.setTitle("LED Control Console (OSC Stub)");
        surface.setResizable(true);
        textFont(createFont("Arial", 13));

        initializeLayout();
        initializeOsc();
        println(oscStatus);
    }

    @Override
    public void draw() {
        if (prevWidth != width || prevHeight != height) {
            prevWidth = width;
            prevHeight = height;
            initializeLayout();
        }


        background(18);

        drawHeader();
        for (Fader fader : faders) {
            fader.draw(this);
        }
        for (ToggleButton button : buttons) {
            button.draw(this);
        }
    }

    @Override
    public void mousePressed() {
        activeFader = -1;

        for (int i = 0; i < faders.length; i++) {
            if (faders[i].contains(mouseX, mouseY)) {
                activeFader = i;
                updateFader(i, mouseY);
                return;
            }
        }

        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].contains(mouseX, mouseY)) {
                buttons[i].toggle();
                sendOscMessage("/led/button/" + buttonNames[i], buttons[i].isOn() ? 1 : 0);
                return;
            }
        }
    }

    @Override
    public void mouseDragged() {
        if (activeFader >= 0) {
            updateFader(activeFader, mouseY);
        }
    }

    @Override
    public void mouseReleased() {
        activeFader = -1;
    }

    private void initializeLayout() {
        float leftMargin = 70;
        float top = 80;
        float faderWidth = 52;
        float faderHeight = max(140, height - 180);
        float spacing = (width - (2 * leftMargin) - (faders.length * faderWidth)) / max(1f, faders.length - 1);

        for (int i = 0; i < faders.length; i++) {
            float x = leftMargin + (i * (faderWidth + spacing));
            if (faders[i] == null) {
                faders[i] = new Fader(faderNames[i], x, top, faderWidth, faderHeight);
            } else {
                faders[i].setBounds(x, top, faderWidth, faderHeight);
            }
        }

        float buttonTop = top + faderHeight + 24;
        float buttonWidth = 90;
        float buttonHeight = 36;
        float buttonSpacing = (width - (2 * leftMargin) - (buttons.length * buttonWidth)) / max(1f, buttons.length - 1);

        for (int i = 0; i < buttons.length; i++) {
            float x = leftMargin + (i * (buttonWidth + buttonSpacing));
            if (buttons[i] == null) {
                buttons[i] = new ToggleButton(buttonNames[i], x, buttonTop, buttonWidth, buttonHeight);
            } else {
                buttons[i].setBounds(x, buttonTop, buttonWidth, buttonHeight);
            }
        }
    }

    private void initializeOsc() {
        String host = System.getProperty("osc.host", "127.0.0.1");
        int port = Integer.getInteger("osc.port", 5142);

        try {
            oscOut = new OSCPortOutBuilder()
                    .setRemoteSocketAddress(new InetSocketAddress(InetAddress.getByName(host), port))
                    .build();
            oscStatus = "OSC target: " + host + ":" + port;
        } catch (IOException ioException) {
            oscOut = null;
            oscStatus = "OSC init failed: " + ioException.getMessage();
            System.err.println(oscStatus);
        }
    }

    private void updateFader(int index, float mouseYPosition) {
        Fader fader = faders[index];
        float newValue = fader.valueFromMouse(mouseYPosition);

        if (newValue != fader.value()) {
            fader.setValue(newValue);
            sendOscMessage("/led/fader/" + faderNames[index], newValue);
        }
    }

    private void sendOscMessage(String address, Object value) {
        if (oscOut == null) {
            return;
        }

        try {
            println("Sending OSC: " + address + " -> " + value);
            oscOut.send(new OSCMessage(address, List.of(value)));
        } catch (IOException | OSCSerializeException ioException) {
            oscStatus = "OSC send failed: " + ioException.getMessage();
            println(oscStatus);
        }
    }

    private void drawHeader() {
        fill(230);
        textAlign(LEFT, TOP);
        textSize(18);
        text("LED Control Console", 20, 16);

        textSize(12);
        fill(170);
        text("Drag faders and click buttons to send OSC values.", 20, 42);

        fill(120, 210, 150);
        textAlign(RIGHT, TOP);
        text(oscStatus, width - 20, 20);
    }

    private static class Fader {
        private final String label;
        private float x;
        private float y;
        private float w;
        private float h;
        private float value;

        private Fader(String label, float x, float y, float w, float h) {
            this.label = label;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.value = 0.5f;
        }

        private void setBounds(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        private void setValue(float value) {
            this.value = value;
        }

        private float value() {
            return value;
        }

        private float valueFromMouse(float mouseY) {
            return constrain(1.0f - ((mouseY - y) / h), 0.0f, 1.0f);
        }

        private boolean contains(float px, float py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }

        private void draw(PApplet p) {
            p.noStroke();
            p.fill(45);
            p.rect(x, y, w, h, 8);

            float filledHeight = value * h;
            p.fill(80, 170, 255);
            p.rect(x, y + h - filledHeight, w, filledHeight, 8);

            p.fill(245);
            p.textAlign(CENTER, BOTTOM);
            p.text(label, x + (w / 2), y - 8);

            p.textAlign(CENTER, TOP);
            p.fill(200);
            p.text(nf(value, 1, 2), x + (w / 2), y + h + 4);
        }
    }

    private static class ToggleButton {
        private final String label;
        private float x;
        private float y;
        private float w;
        private float h;
        private boolean on;

        private ToggleButton(String label, float x, float y, float w, float h) {
            this.label = label;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        private void setBounds(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        private void toggle() {
            on = !on;
        }

        private boolean isOn() {
            return on;
        }

        private boolean contains(float px, float py) {
            return px >= x && px <= x + w && py >= y && py <= y + h;
        }

        private void draw(PApplet p) {
            p.stroke(70);
            p.fill(on ? p.color(70, 210, 110) : p.color(70));
            p.rect(x, y, w, h, 6);

            p.fill(255);
            p.textAlign(CENTER, CENTER);
            p.text(label + (on ? " ON" : " OFF"), x + (w / 2), y + (h / 2));
        }
    }
}
