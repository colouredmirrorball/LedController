package be.cmbsoft.ledcontrol;

import be.cmbsoft.ledcontrol.input.Blackout;
import be.cmbsoft.ledcontrol.input.Input;
import be.cmbsoft.ledcontrol.input.ScreenGrabber;
import be.cmbsoft.ledcontrol.input.StaticColour;
import be.cmbsoft.ledcontrol.output.*;
import be.cmbsoft.ledcontrol.ui.StripConfigPanel;
import com.illposed.osc.MessageSelector;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCMessageEvent;
import com.illposed.osc.OSCMessageListener;
import com.illposed.osc.transport.OSCPortIn;
import com.illposed.osc.transport.OSCPortInBuilder;
import processing.core.PApplet;
import processing.core.PGraphics;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class LedController extends PApplet implements OSCMessageListener {

    public static final String WIDTH_KEY = "width";
    public static final String HEIGHT_KEY = "height";

    private static final String STRIP_CONFIG_PATH = "src/main/resources/strips.json";

    private final Properties properties = new Properties();
    private final List<AbstractOutput> outputs = new ArrayList<>();
    private final int outputWidth;
    private final int outputHeight;
    private final OSCPortIn port;
    private final LedStripConfig stripConfig;

    private PGraphics matrix;
    private Map<Character, Input> inputs;
    private Input activeInput;
    private StripConfigPanel configPanel;
    private boolean editMode;
    private float intensity = 1f;


    public LedController() {
        try (InputStream propertiesStream = new FileInputStream("src/main/resources/settings.properties")) {

            properties.load(propertiesStream);
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
            port = new OSCPortInBuilder()
                    .setLocalPort(Integer.parseInt(properties.getProperty("OscPort", "5142")))
                    .addMessageListener(selector, this)
                    .build();
            port.startListening();
            outputWidth = parseOutputWidth();
            outputHeight = parseOutputHeight();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        stripConfig = new LedStripConfig(new File(STRIP_CONFIG_PATH));
    }

    public static void main(String[] args) {
        runSketch(new String[]{LedController.class.getPackageName()}, new LedController());
    }

    @Override
    public void settings() {
        size(outputWidth + 600, outputHeight + 200);
        noSmooth();
    }

    private int parseOutputHeight() {
        return Integer.parseInt(properties.getProperty(HEIGHT_KEY, "16"));
    }

    private int parseOutputWidth() {
        return Integer.parseInt(properties.getProperty(WIDTH_KEY, "120"));
    }

    @Override
    public void setup() {
        // Build outputs from persisted strip config
        rebuildOutputs(stripConfig.reload().getStrips());

        // Open the strip configuration panel (on the Swing EDT)
        SwingUtilities.invokeLater(() -> {
            configPanel = new StripConfigPanel(stripConfig);
            configPanel.setOnStripsChanged(strips -> {
                stripConfig.save();
                rebuildOutputs(stripConfig.getStrips());
            });
            configPanel.setVisible(true);
        });

        inputs = new HashMap<>();
        inputs.put('s', new ScreenGrabber());
        inputs.put('c', new StaticColour());
        inputs.put('b', new Blackout());
        activeInput = inputs.get('b');
        background(0);
        matrix = createGraphics(parseOutputWidth(), parseOutputHeight());
    }

    /**
     * Replaces the current outputs with fresh {@link ArtNetOutput} instances.
     */
    private synchronized void rebuildOutputs(List<LedStrip> strips) {
        outputs.clear();
        for (LedStrip strip : strips) {
            outputs.add(new ArtNetOutput(strip));
        }
        println("Outputs rebuilt: " + outputs.size() + " strip(s).");
    }

    @Override
    public void draw() {
        background(50);
        matrix.beginDraw();
        activeInput.drawGraphics(matrix, this);
        matrix.endDraw();

        matrix.loadPixels();
        image(matrix, 50, 20, outputWidth, outputHeight);

        drawStripOverlays();
        processOutputs();
    }

    /**
     * Draws a visual overlay for each strip so the user can see their positions.
     */
    private void drawStripOverlays() {
        List<LedStrip> strips = stripConfig.getStrips();
        for (LedStrip strip : strips) {
            noStroke();
            for (Pixel pixel : strip.getPixels()) {
                fill(color(pixel.red(), pixel.green(), pixel.blue()));
                rect(pixel.x(), pixel.y(), 4, 4);
            }
            if (editMode) {
                // Show strip overlays on the canvas
            }
        }
    }

    @Override
    public void keyPressed() {
        if (key == 'e' || key == 'E') {
            editMode = !editMode;
            // Toggle strip config window
            SwingUtilities.invokeLater(() -> {
                if (configPanel != null) {
                    configPanel.setVisible(editMode);
                }
            });
        } else if (inputs.containsKey(key)) {
            activeInput = inputs.get(key);
        }
    }

    private synchronized void processOutputs() {
        for (AbstractOutput output : outputs) {
            output.send(this::getPixel);
        }
    }

    public byte[] getPixel(int x, int y) {
        byte[] output = new byte[4];
        int pixelIndex = x + y * matrix.width;

        int c = matrix.pixels[pixelIndex];

        int r = (c >>> 16) & 0xFF;
        int g = (c >>> 8) & 0xFF;
        int b = c & 0xFF;

        output[0] = (byte) (r * intensity);
        output[1] = (byte) (g * intensity);
        output[2] = (byte) (b * intensity);

        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        float saturation = max == 0 ? 0f : (max - min) * 255f / max;
        int calculatedSaturation = saturation < 25f
                ? Math.min(255, max * (((int) (255f - 10f * saturation)) & 0xFF) / 255)
                : 0;

        output[3] = (byte) (calculatedSaturation * intensity);

        return output;
    }

    @Override
    public void acceptMessage(OSCMessageEvent oscMessageEvent) {
        OSCMessage message = oscMessageEvent.getMessage();
        String address = message.getAddress();
        CharSequence argumentTypeTags = message.isInfoSet() ? message.getInfo().getArgumentTypeTags() : "";
        println("Got OSC message: ", address, argumentTypeTags, message.getArguments());

        switch (address) {
            case "/led/fader/Intensity":
                if (argumentTypeTags.charAt(0) == 'f') {
                    intensity = (Float) message.getArguments().get(0);
                }
        }
    }

    @Override
    public void exit() {
        if (port != null) {
            port.stopListening();
        }
        super.exit();
    }

    @FunctionalInterface
    public interface PixelFetcher {
        byte[] getData(int x, int y);
    }
}
