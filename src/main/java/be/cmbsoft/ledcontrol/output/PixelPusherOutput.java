package be.cmbsoft.ledcontrol.output;

import be.cmbsoft.ledcontrol.LedController;
import com.heroicrobot.dropbit.devices.pixelpusher.Strip;
import com.heroicrobot.dropbit.registry.DeviceRegistry;

import java.util.Observable;
import java.util.Observer;

import static processing.core.PApplet.println;

public class PixelPusherOutput {

    private final LedController ledController;
    DeviceRegistry registry;
    TestObserver testObserver;

    public PixelPusherOutput(LedController ledController) {
        registry = new DeviceRegistry();
        testObserver = new TestObserver();
        registry.addObserver(testObserver);
        this.ledController = ledController;
    }

    public void sendPixels(PixelFetcher fetcher) {
        if (testObserver.hasStrips()) {
            registry.startPushing();
            int currentStrip = 0;
            int stride = 16;    //TODO make configurable
            for (Strip strip : registry.getStrips()) {
                for (int index = 0; index < strip.getLength(); index++) {
                    int xPos = index / stride + stride * currentStrip;
                    boolean odd = xPos % 2 == 1;
                    int yPos = odd ? stride - index % stride : index % stride;
                    int pixel = fetcher.getPixel(xPos, yPos);
                    int corrected = ledController.color(ledController.red(pixel), ledController.blue(pixel),
                            ledController.green(pixel));
                    strip.setPixel(corrected, index);
                }
                currentStrip++;
            }
        }
    }

    @FunctionalInterface
    public interface PixelFetcher {
        int getPixel(int x, int y);
    }

    static class TestObserver implements Observer {
        private boolean hasStrips = false;

        public void update(Observable registry, Object updatedDevice) {
            println("Registry changed!");
            if (updatedDevice != null) {
                println("Device change: " + updatedDevice);
            }
            this.hasStrips = true;
        }

        public boolean hasStrips() {
            return hasStrips;
        }
    }
}
