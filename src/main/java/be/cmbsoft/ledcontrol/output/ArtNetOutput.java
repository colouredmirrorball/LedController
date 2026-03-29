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

        int index = 0;
        for (Pixel pixel : strip.getPixels()) {
            if (index >= maxLeds)
                break;
            int x = pixel.x();
            int y = pixel.y();
            // Fetch a single pixel: 1×1 region at (cx, cy)
            byte[] px = fetcher.getData(x, y);
            int base = index * 4;

            dmx[base] = px[1]; // G
            dmx[base + 1] = px[0]; // R
            dmx[base + 2] = px[2]; // B
            dmx[base + 3] = px[3]; // W
            pixel.setRed(px[0] & 0xFF);
            pixel.setGreen(px[1] & 0xFF);
            pixel.setBlue(px[2] & 0xFF);
            index++;
        }

        artNetClient.unicastDmx(
                strip.getRemoteIp(),
                strip.getSubnet(),
                strip.getUniverse(),
                dmx);
    }
}
