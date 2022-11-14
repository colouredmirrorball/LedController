package be.cmbsoft.ledcontrol;

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
import java.util.Properties;

public class LedController extends PApplet implements OSCMessageListener {

    private final OSCPortIn port;
    private final ArtNetClient artNetClient;
    private final Properties properties = new Properties();
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

            port = new OSCPortInBuilder()
                    .setLocalPort(Integer.parseInt(properties.getProperty("OscPort", "5142")))
                    .addMessageListener(selector, this)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        artNetClient = new ArtNetClient();
        artNetClient.start();
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
        matrix = createGraphics(Integer.parseInt(properties.getProperty("width", "16")),
                Integer.parseInt(properties.getProperty("height", "120")));
    }

    @Override
    public void draw() {
        matrix.fill(255);
    }

    @Override
    public void acceptMessage(OSCMessageEvent oscMessageEvent) {
        System.out.println(oscMessageEvent.getMessage());
    }
}
