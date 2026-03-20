package be.cmbsoft.ledcontrol;

import be.cmbsoft.ledcontrol.input.Input;
import be.cmbsoft.ledcontrol.input.ScreenGrabber;
import be.cmbsoft.ledcontrol.output.AbstractOutput;
import be.cmbsoft.ledcontrol.output.ArtNetOutput;
import be.cmbsoft.ledcontrol.output.OutputType;
import be.cmbsoft.ledcontrol.output.PixelPusherOutput;
import com.illposed.osc.MessageSelector;
import com.illposed.osc.OSCMessageEvent;
import com.illposed.osc.OSCMessageListener;
import com.illposed.osc.transport.OSCPortIn;
import com.illposed.osc.transport.OSCPortInBuilder;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class LedController extends PApplet implements OSCMessageListener {

    public static final String WIDTH_KEY = "width";
    public static final String HEIGHT_KEY = "height";
    private static final OutputType outputType = OutputType.PIXELPUSHER;
    private final Properties properties = new Properties();
    private final List<AbstractOutput> outputs = new ArrayList<>();
    private final int stride;
    private final int outputWidth;
    private PGraphics matrix;
    private final int outputHeight;
    private final OSCPortIn port;
    private Input input;

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
            port = new OSCPortInBuilder().setLocalPort(Integer.parseInt(properties.getProperty("OscPort", "5142")))
                    .addMessageListener(selector, this).build();
            stride = Integer.parseInt(properties.getProperty("stride", "10"));
            outputWidth = parseOutputWidth();
            outputHeight = parseOutputHeight();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    //private PixelPusherOutput pixelPusherOutput = null;

    public static void main(String[] args) {
        runSketch(new String[]{LedController.class.getPackageName()}, new LedController());
    }

    @Override
    public void settings() {
        size(outputWidth * 10, outputHeight * 10);
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

        if (outputType == OutputType.PIXELPUSHER) {
            setupPixelPusherOutput();
        } else if (outputType == OutputType.ART_NET) {
            setupArtNetOutput();
        }

        input = new ScreenGrabber();
        background(0);
        matrix = createGraphics(Integer.parseInt(properties.getProperty(WIDTH_KEY, "256")),
                parseOutputHeight());
    }

    private void setupPixelPusherOutput() {
        outputs.add(new PixelPusherOutput(this));
    }

    private void setupArtNetOutput() {
        String remoteIp = properties.getProperty("remoteIp", "127.0.0.1");
        int remotePort = Integer.parseInt(properties.getProperty("remotePort", "6454"));

        for (int i = 0; i < 16; i++) {
            outputs.add(new ArtNetOutput(remoteIp, remotePort, 0, i, 0, i, 120, 1));
        }
    }

    @Override
    public void draw() {
        matrix.beginDraw();
        input.drawGraphics(matrix, this);
        matrix.endDraw();

        matrix.loadPixels();
        image(matrix, 0, 0, width, height);
        processOutputs();
        fill(0);
        rect(5, 5, 50, 25);
        fill(255);
        text("FPS: " + (int) frameRate, 10, 25);
    }

    private void processOutputs() {
        for (AbstractOutput output : outputs) {
            output.send(this::getReshuffledBytes);
        }
    }

    private byte[] getReshuffledBytes(int x, int y, int width, int height) {
        byte[] output = new byte[512];
        int index = 0;
        width = min(width, outputWidth);
        for (int i = x, endX = x + width, endY = y + height, matrixWidth = matrix.width; i < endX; i++) {
            int pixelIndex = y * matrixWidth + i;
            for (int j = y; j < endY; j++, pixelIndex += matrixWidth) {
                if (index >= 508) {
                    println("Error: index out of bounds avoided!");
                    println("index", index, "x", x, "y", y, WIDTH_KEY, width, HEIGHT_KEY, height);
                    break;
                }
                int c = matrix.pixels[pixelIndex];

                int r = (c >>> 16) & 0xFF;
                int g = (c >>> 8) & 0xFF;
                int b = c & 0xFF;

                output[index++] = (byte) r;
                output[index++] = (byte) g;
                output[index++] = (byte) b;

                int max = Math.max(r, Math.max(g, b));
                int min = Math.min(r, Math.min(g, b));
                float saturation = max == 0 ? 0f : (max - min) * 255f / max;

                output[index++] = saturation < 25f
                        ? (byte) (max * (((int) (255f - 10f * saturation)) & 0xFF))
                        : 0;
            }
        }
        return output;
    }

    @Override
    public void acceptMessage(OSCMessageEvent oscMessageEvent) {
        System.out.println(oscMessageEvent.getMessage());
    }

    public int getStride() {
        return stride;
    }

    @FunctionalInterface
    public interface PixelFetcher {
        byte[] getData(int x, int y, int width, int height);
    }
}
