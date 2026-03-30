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

    /**
     * Half-size of the draggable handle rectangles in screen pixels.
     */
    private static final int HANDLE_HALF = 6;
    private final int canvasX = 50;

    // ---- edit-mode drag state ----
    private final int canvasY = 50;
    /**
     * Index of the strip whose handle is being dragged, or -1.
     */
    private int dragStripIndex = -1;
    /**
     * Which handle: 0 = start, 1 = end.
     */
    private int dragHandle = -1;


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
        for (AbstractOutput output : outputs) {
            output.stop();
        }
        outputs.clear();
        for (LedStrip strip : strips) {
            outputs.add(new ArtNetOutput(strip));
        }
        println("Outputs rebuilt: " + outputs.size() + " strip(s).");
    }

    @Override
    public void draw() {
        try {
            background(50);
            matrix.beginDraw();
            activeInput.drawGraphics(matrix, this);
            matrix.endDraw();

            matrix.loadPixels();
            image(matrix, canvasX, canvasY, outputWidth, outputHeight);

            drawStripOverlays();
            processOutputs();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Computes the canvas-space end position of a strip.
     * End = start + direction * spacing * (ledCount - 1)
     */
    private static double[] stripEndPosition(LedStrip strip) {
        double angleRad = Math.toRadians(strip.getAngleDegrees());
        double span = strip.getLedSpacingPixels() * Math.max(strip.getLedCount() - 1, 0);
        return new double[]{
                strip.getStartX() + Math.cos(angleRad) * span,
                strip.getStartY() + Math.sin(angleRad) * span
        };
    }

    /**
     * Draws a visual overlay for each strip so the user can see their positions.
     * In edit mode, also draws draggable start (green) and end (red) handles.
     */
    private void drawStripOverlays() {
        List<LedStrip> strips = stripConfig.getStrips();
        if (editMode) {
            strokeWeight(2);
            stroke(255, 127, 0);
        } else {
            noStroke();
        }
        for (LedStrip strip : strips) {
            for (Pixel pixel : strip.getPixels()) {
                fill(color(pixel.red(), pixel.green(), pixel.blue()));
                rect(map(pixel.x(), 0, outputWidth, canvasX, canvasX + outputWidth),
                        map(pixel.y(), 0, outputHeight, canvasY, canvasY + outputHeight), 6, 6);
            }
            if (editMode) {
                // Compute screen-space positions of the start and end handles
                float sx = stripToScreenX(strip.getStartX());
                float sy = stripToScreenY(strip.getStartY());

                double[] end = stripEndPosition(strip);
                float ex = stripToScreenX(end[0]);
                float ey = stripToScreenY(end[1]);

                // Draw line connecting start to end
                stroke(255, 200, 0);
                strokeWeight(1);
                line(sx, sy, ex, ey);

                // Start handle – green square
                strokeWeight(2);
                stroke(0, 220, 0);
                fill(0, 255, 0, 180);
                rect(sx - HANDLE_HALF, sy - HANDLE_HALF, (float) HANDLE_HALF * 2, (float) HANDLE_HALF * 2);

                // End handle – red square
                stroke(220, 0, 0);
                fill(255, 0, 0, 180);
                rect(ex - HANDLE_HALF, ey - HANDLE_HALF, (float) HANDLE_HALF * 2, (float) HANDLE_HALF * 2);
            }
        }
    }

    /**
     * Maps a strip canvas X coordinate to a Processing screen X coordinate.
     */
    private float stripToScreenX(double canvasCoord) {
        return map((float) canvasCoord, 0, outputWidth, canvasX, canvasX + (float) outputWidth);
    }

    /**
     * Maps a strip canvas Y coordinate to a Processing screen Y coordinate.
     */
    private float stripToScreenY(double canvasCoord) {
        return map((float) canvasCoord, 0, outputHeight, canvasY, canvasY + (float) outputHeight);
    }

    /**
     * Maps a Processing screen X to a strip canvas X coordinate.
     */
    private double screenToStripX(float screenX) {
        return map(screenX, canvasX, canvasX + (float) outputWidth, 0, outputWidth);
    }

    /**
     * Maps a Processing screen Y to a strip canvas Y coordinate.
     */
    private double screenToStripY(float screenY) {
        return map(screenY, canvasY, canvasY + (float) outputHeight, 0, outputHeight);
    }

    @Override
    public void mousePressed() {
        if (editMode) {
            List<LedStrip> strips = stripConfig.getStrips();
            for (int i = 0; i < strips.size(); i++) {
                LedStrip strip = strips.get(i);

                // Check start handle
                float sx = stripToScreenX(strip.getStartX());
                float sy = stripToScreenY(strip.getStartY());
                if (mouseOver(sx, sy)) {
                    dragStripIndex = i;
                    dragHandle = 0;
                    return;
                }

                // Check end handle
                double[] end = stripEndPosition(strip);
                float ex = stripToScreenX(end[0]);
                float ey = stripToScreenY(end[1]);
                if (mouseOver(ex, ey)) {
                    dragStripIndex = i;
                    dragHandle = 1;
                    return;
                }
            }
        }
    }

    private boolean mouseOver(float sx, float sy) {
        return Math.abs(mouseX - sx) <= HANDLE_HALF && Math.abs(mouseY - sy) <= HANDLE_HALF;
    }

    @Override
    public void mouseDragged() {
        if (editMode && dragStripIndex >= 0) {
            LedStrip strip = stripConfig.getStrips().get(dragStripIndex);

            double mx = screenToStripX(mouseX);
            double my = screenToStripY(mouseY);

            if (dragHandle == 0) {
                // Moving the start handle – preserve angle & spacing, just translate
                strip.setStartX(mx);
                strip.setStartY(my);
            } else {
                // Moving the end handle – recompute angle and spacing
                double dx = mx - strip.getStartX();
                double dy = my - strip.getStartY();
                double dist = Math.sqrt(dx * dx + dy * dy);
                double newAngle = Math.toDegrees(Math.atan2(dy, dx));
                strip.setAngleDegrees(newAngle);
                int ledCount = strip.getLedCount();
                if (ledCount > 1) {
                    strip.setLedSpacingPixels(dist / (ledCount - 1));
                }
            }

            // Refresh pixel positions so the overlay stays in sync
            strip.getPixels().clear();
            strip.updatePixelPositions();

            // Keep the Swing table in sync while dragging
            if (configPanel != null) {
                SwingUtilities.invokeLater(configPanel::refreshTable);
            }
        }
    }

    @Override
    public void mouseReleased() {
        if (dragStripIndex >= 0) {
            // Persist the updated strip config
            stripConfig.save();
            if (configPanel != null) {
                SwingUtilities.invokeLater(configPanel::refreshTable);
            }
            dragStripIndex = -1;
            dragHandle = -1;
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
        int c = (x <= outputWidth && y <= outputHeight)  ? matrix.pixels[pixelIndex] : 0;

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
        for (AbstractOutput output : outputs) {
            output.stop();
        }
        super.exit();
    }

    @FunctionalInterface
    public interface PixelFetcher {
        byte[] getData(int x, int y);
    }
}
