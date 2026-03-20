package be.cmbsoft.ledcontrol.output;

import be.cmbsoft.ledcontrol.LedController;
import com.heroicrobot.dropbit.devices.pixelpusher.Strip;
import com.heroicrobot.dropbit.registry.DeviceRegistry;

import java.util.Observable;
import java.util.Observer;

import static processing.core.PApplet.println;

public class PixelPusherOutput extends AbstractOutput {

    private final LedController ledController;
    DeviceRegistry registry;
    TestObserver testObserver;

    public PixelPusherOutput(LedController ledController) {
        registry = new DeviceRegistry();
        testObserver = new TestObserver();
        registry.addObserver(testObserver);
        this.ledController = ledController;
    }

    @Override
    public void send(LedController.PixelFetcher fetcher) {
        if (testObserver.hasStrips()) {
            registry.startPushing();
            registry.setExtraDelay(0);
            registry.setAutoThrottle(true);
            int currentStrip = 0;
            for (Strip strip : registry.getStrips()) {
                byte[] data = fetcher.getData(0, currentStrip, strip.getLength(), 1);
                int max = Math.min(data.length / 3, strip.getLength());
                for (int index = 0; index < max; index++) {
//                    int xPos = index / stride + stride * currentStrip;
//                    boolean odd = xPos % 2 == 1;
//                    int yPos = odd ? stride - index % stride : index % stride;
//                    int pixel = fetcher.getData(xPos, yPos);
//                    int corrected = ledController.color(ledController.red(pixel), ledController.blue(pixel),
//                            ledController.green(pixel));
                    int corrected = ledController.color(ledController.red(data[index * 3]),
                            ledController.blue(data[index * 3 + 1]), ledController.green(data[index * 3 + 2]));
                    strip.setPixel(corrected, index);
                }
                currentStrip++;
            }
            //registry.stopPushing();
        }
    }

    static class TestObserver implements Observer {
        private boolean hasStrips = false;

        @Override
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
