package be.cmbsoft.ledcontrol.output;

import be.cmbsoft.ledcontrol.LedController;
import ch.bildspur.artnet.ArtNetClient;

import java.util.Objects;

public final class ArtNetOutput extends AbstractOutput {
    private final String ip;
    private final int port;
    private final int subnet;
    private final int universe;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final ArtNetClient artNetClient;

    public ArtNetOutput(String ip, int port, int subnet, int universe, int x, int y, int width, int height) {
        this.ip = ip;
        this.port = port;
        this.subnet = subnet;
        this.universe = universe;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        artNetClient = new ArtNetClient();
        artNetClient.start();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var that = (ArtNetOutput) obj;
        return Objects.equals(this.ip, that.ip) &&
                this.port == that.port &&
                this.subnet == that.subnet &&
                this.universe == that.universe &&
                this.x == that.x &&
                this.y == that.y &&
                this.width == that.width &&
                this.height == that.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port, subnet, universe, x, y, width, height);
    }

    @Override
    public String toString() {
        return "ArtNetOutput[" +
                "ip=" + ip + ", " +
                "port=" + port + ", " +
                "subnet=" + subnet + ", " +
                "universe=" + universe + ", " +
                "x=" + x + ", " +
                "y=" + y + ", " +
                "width=" + width + ", " +
                "height=" + height + ']';
    }

    @Override
    public void send(LedController.PixelFetcher fetcher) {
        artNetClient.unicastDmx(ip, subnet, universe, fetcher.getData(x, y, width, height));
    }
}
