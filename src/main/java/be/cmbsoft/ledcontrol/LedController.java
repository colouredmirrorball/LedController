package be.cmbsoft.ledcontrol;

import be.cmbsoft.ledcontrol.input.Blackout;
import be.cmbsoft.ledcontrol.input.Input;
import be.cmbsoft.ledcontrol.input.ScreenGrabber;
import be.cmbsoft.ledcontrol.input.StaticColour;
import be.cmbsoft.ledcontrol.output.AbstractOutput;
import be.cmbsoft.ledcontrol.output.ArtNetOutput;
import be.cmbsoft.ledcontrol.output.LedStrip;
import be.cmbsoft.ledcontrol.output.LedStripConfig;
import be.cmbsoft.ledcontrol.ui.StripConfigPanel;
import com.illposed.osc.MessageSelector;
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
        size(outputWidth, outputHeight);
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
        rebuildOutputs(stripConfig.load());

        // Open the strip configuration panel (on the Swing EDT)
        SwingUtilities.invokeLater(() -> {
            configPanel = new StripConfigPanel(stripConfig);
            configPanel.setOnStripsChanged(strips -> {
                stripConfig.save(strips);
                rebuildOutputs(strips);
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
        matrix.beginDraw();
        activeInput.drawGraphics(matrix, this);
        matrix.endDraw();

        matrix.loadPixels();
        image(matrix, 0, 0, width, height);

        if (editMode) {
            // Show strip overlays on the canvas
            drawStripOverlays();
        }

        processOutputs();
    }

    /**
     * Draws a visual overlay for each strip so the user can see their positions.
     */
    private void drawStripOverlays() {
        List<LedStrip> strips = configPanel.getStrips();
        for (LedStrip strip : strips) {
            double angleRad = Math.toRadians(strip.getAngleDegrees());
            double cosA = Math.cos(angleRad);
            double sinA = Math.sin(angleRad);
            double spacing = strip.getLedSpacingPixels();
            // Scale from matrix coords to screen coords
            float scaleX = (float) width / outputWidth;
            float scaleY = (float) height / outputHeight;

            stroke(255, 255, 0);
            strokeWeight(1);
            noFill();
            for (int i = 0; i < strip.getLedCount(); i++) {
                float sx = (float) (strip.getStartX() + cosA * spacing * i) * scaleX;
                float sy = (float) (strip.getStartY() + sinA * spacing * i) * scaleY;
                ellipse(sx, sy, 4, 4);
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

    /**
     * Returns the RGB (and optional white) bytes for a region of the matrix.
     *
     * <p>When {@code width == 1 && height == 1} (single-LED lookup) this method
     * returns exactly 3 bytes: {@code [R, G, B]}.  For larger regions it falls
     * back to the legacy DMX-packed layout.
     */
//    private byte[] getPixelRgb(int x, int y, int w, int h) {
//        if (w == 1 && h == 1) {
//            // Fast single-pixel path used by ArtNetOutput strip sampling
//            int px = x + y * matrix.width;
//            if (px < 0 || px >= matrix.pixels.length) return new byte[3];
//            int c = matrix.pixels[px];
//            return new byte[]{
//                (byte) ((c >>> 16) & 0xFF),
//                (byte) ((c >>>  8) & 0xFF),
//                (byte) ( c        & 0xFF)
//            };
//        }
//        return getReshuffledBytes(x, y, w, h);
//    }
    public byte[] getPixel(int x, int y) {
        byte[] output = new byte[4];
        int pixelIndex = x + y * matrix.width;

        int c = matrix.pixels[pixelIndex];

        int r = (c >>> 16) & 0xFF;
        int g = (c >>> 8) & 0xFF;
        int b = c & 0xFF;

        output[0] = (byte) r;
        output[1] = (byte) g;
        output[2] = (byte) b;

        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        float saturation = max == 0 ? 0f : (max - min) * 255f / max;
        int calculatedSaturation = saturation < 25f
                ? Math.min(255, max * (((int) (255f - 10f * saturation)) & 0xFF) / 255)
                : 0;

        output[3] = (byte) calculatedSaturation;

        return output;
    }

    @Override
    public void acceptMessage(OSCMessageEvent oscMessageEvent) {
        System.out.println(oscMessageEvent.getMessage());
    }

    @FunctionalInterface
    public interface PixelFetcher {
        byte[] getData(int x, int y);
    }
}
