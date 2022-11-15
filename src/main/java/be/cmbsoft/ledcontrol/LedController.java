package be.cmbsoft.ledcontrol;

import be.cmbsoft.ledcontrol.output.ArtNetOutput;
import ch.bildspur.artnet.ArtNetClient;
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

    private final OSCPortIn port;
    private final ArtNetClient artNetClient;
    private final Properties properties = new Properties();
    private final List<ArtNetOutput> outputs = new ArrayList<>();
    private PGraphics matrix;

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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        artNetClient = new ArtNetClient();
        artNetClient.start();

        String remoteIp = properties.getProperty("remoteIp", "127.0.0.1");
        int remotePort = Integer.parseInt(properties.getProperty("remotePort", "6454"));

        for (int i = 0; i < 16; i++) {
            outputs.add(new ArtNetOutput(remoteIp, remotePort, i, 0, i, 0, 1, 120));
        }
    }

    public static void main(String[] args) {
        runSketch(new String[]{LedController.class.getPackageName()}, new LedController());
    }

    @Override
    public void settings() {
        size(1200, 800);
    }

    @Override
    public void setup() {
        background(0);
        matrix = createGraphics(Integer.parseInt(properties.getProperty("width", "16")),
                Integer.parseInt(properties.getProperty("height", "120")));
    }

    @Override
    public void draw() {
        matrix.fill(255);

        matrix.loadPixels();
        processOutputs();
    }

    private void processOutputs() {
        for (ArtNetOutput output : outputs) {
            artNetClient.unicastDmx(output.ip(), output.subnet(), output.universe(),
                    getData(output.x(), output.y(), output.width(), output.height()));
        }
    }

    private byte[] getData(int x, int y, int width, int height) {
        byte[] output = new byte[512];
        int index = 0;
        for (int i = x; i < x + width; i++) {
            for (int j = y; j < y + height; j++) {
                int c = matrix.get(i, j);
                output[index++] = (byte) (((int) red(c)) & 0xff);
                output[index++] = (byte) (((int) green(c)) & 0xff);
                output[index++] = (byte) (((int) blue(c)) & 0xff);
                float saturation = saturation(c);
                output[index++] = saturation < 25 ? (byte) (((int) (255 - 25 * saturation)) & 0xff) : 0;
            }
        }
        return output;
    }

    @Override
    public void acceptMessage(OSCMessageEvent oscMessageEvent) {
        System.out.println(oscMessageEvent.getMessage());
    }
}
