package be.cmbsoft.ledcontrol.output;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes a single LED strip placed in the canvas.
 *
 * <p>The strip starts at ({@code startX}, {@code startY}) and extends at
 * {@code angleDegrees} for {@code ledCount} LEDs, each spaced
 * {@code ledSpacingPixels} pixels apart.
 * Art-Net addressing: {@code subnet} + {@code universe} on {@code remoteIp}:{@code remotePort}.
 */
public class LedStrip {

    /**
     * Human-readable label shown in the UI.
     */
    @JsonProperty
    private String name = "Strip";

    /**
     * Canvas X coordinate of the first LED (pixels).
     */
    @JsonProperty
    private double startX = 0;

    /**
     * Canvas Y coordinate of the first LED (pixels).
     */
    @JsonProperty
    private double startY = 0;

    /**
     * Direction the strip extends, in degrees clockwise from the positive X axis.
     */
    @JsonProperty
    private double angleDegrees = 0;

    /**
     * Number of LEDs on this strip.
     */
    @JsonProperty
    private int ledCount = 60;

    /**
     * Distance between consecutive LEDs in canvas pixels.
     */
    @JsonProperty
    private double ledSpacingPixels = 1.0;

    // ---- Art-Net routing ----
    @JsonProperty
    private String remoteIp = "127.0.0.1";
    @JsonProperty
    private int remotePort = 6454;
    @JsonProperty
    private int subnet = 0;
    @JsonProperty
    private int universe = 0;

    // ---- constructors ----

    /**
     * Default no-arg constructor required by Jackson.
     */
    public LedStrip() {
    }

    public LedStrip(String name, double startX, double startY, double angleDegrees,
                    int ledCount, double ledSpacingPixels,
                    String remoteIp, int remotePort, int subnet, int universe) {
        this.name = name;
        this.startX = startX;
        this.startY = startY;
        this.angleDegrees = angleDegrees;
        this.ledCount = ledCount;
        this.ledSpacingPixels = ledSpacingPixels;
        this.remoteIp = remoteIp;
        this.remotePort = remotePort;
        this.subnet = subnet;
        this.universe = universe;
    }

    // ---- getters / setters ----

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getStartX() {
        return startX;
    }

    public void setStartX(double startX) {
        this.startX = startX;
    }

    public double getStartY() {
        return startY;
    }

    public void setStartY(double startY) {
        this.startY = startY;
    }

    public double getAngleDegrees() {
        return angleDegrees;
    }

    public void setAngleDegrees(double angleDegrees) {
        this.angleDegrees = angleDegrees;
    }

    public int getLedCount() {
        return ledCount;
    }

    public void setLedCount(int ledCount) {
        this.ledCount = ledCount;
    }

    public double getLedSpacingPixels() {
        return ledSpacingPixels;
    }

    public void setLedSpacingPixels(double ledSpacingPixels) {
        this.ledSpacingPixels = ledSpacingPixels;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public int getSubnet() {
        return subnet;
    }

    public void setSubnet(int subnet) {
        this.subnet = subnet;
    }

    public int getUniverse() {
        return universe;
    }

    public void setUniverse(int universe) {
        this.universe = universe;
    }

    @Override
    public String toString() {
        return name + " (" + ledCount + " LEDs, universe " + universe + ")";
    }
}

