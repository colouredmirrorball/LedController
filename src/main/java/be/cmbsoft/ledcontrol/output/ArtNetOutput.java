package be.cmbsoft.ledcontrol.output;

import be.cmbsoft.ledcontrol.LedController;
import ch.bildspur.artnet.ArtNetClient;

import java.util.Objects;

/**
 * Sends one {@link LedStrip}'s worth of pixel data via Art-Net unicast.
 *
 * <p>Pixel colours are sampled from the canvas along the strip's geometric path
 * (start position + angle + per-LED spacing) using the supplied
 * {@link LedController.PixelFetcher}.
 */
public final class ArtNetOutput extends AbstractOutput {

    private final LedStrip strip;
    private final ArtNetClient artNetClient;

    public ArtNetOutput(LedStrip strip) {
        this.strip = strip;
        artNetClient = new ArtNetClient();
        artNetClient.start();
    }

    /**
     * Convenience factory that mirrors the original rectangular constructor.
     */
    public static ArtNetOutput forRectangle(String ip, int port, int subnet, int universe,
                                            int x, int y, int width, int height) {
        LedStrip s = new LedStrip(
                "universe-" + universe,
                x, y, 0.0,
                width * height, 1.0,
                ip, port, subnet, universe);
        return new ArtNetOutput(s);
    }

    public LedStrip getStrip() {
        return strip;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof ArtNetOutput that))
            return false;
        return Objects.equals(this.strip, that.strip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strip);
    }

    @Override
    public String toString() {
        return "ArtNetOutput[" + strip + "]";
    }

    /**
     * Samples each LED's colour from the canvas and sends up to 170 RGB triplets
     * (one full Art-Net DMX universe = 512 bytes) via unicast.
     *
     * <p>If the strip has more than 170 LEDs, only the first 170 are transmitted
     * in this universe; additional universes require separate {@code ArtNetOutput}
     * instances (not yet implemented in this draft).
     */
    @Override
    public void send(LedController.PixelFetcher fetcher) {
        int ledCount = strip.getLedCount();
        // Max 170 RGB LEDs per DMX universe (510 bytes)
        int maxLeds = Math.min(ledCount, 170);
        byte[] dmx = new byte[512];

        double angleRad = Math.toRadians(strip.getAngleDegrees());
        double cosA = Math.cos(angleRad);
        double sinA = Math.sin(angleRad);
        double spacing = strip.getLedSpacingPixels();

        for (int i = 0; i < maxLeds; i++) {
            double cx = strip.getStartX() + cosA * spacing * i;
            double cy = strip.getStartY() + sinA * spacing * i;
            // Fetch a single pixel: 1×1 region at (cx, cy)
            byte[] px = fetcher.getData((int) Math.round(cx), (int) Math.round(cy));
            int base = i * 4;

            dmx[base] = px[1]; // G
            dmx[base + 1] = px[0]; // R
            dmx[base + 2] = px[2]; // B
            dmx[base + 3] = px[3]; // W

        }

//        for (int i = 0; i < 512; i++) {
//            dmx[i] = (byte) 255;
//        }

        artNetClient.unicastDmx(
                strip.getRemoteIp(),
                strip.getSubnet(),
                strip.getUniverse(),
                dmx);
    }
}
