package daemon.keyboardEffects;

import core.DaemonState;
import core.RGBMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Keyboard implements Runnable {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Future<?> activeMode;

    @SuppressWarnings("BusyWait")
    @Override
    public void run() {
        applyMode(DaemonState.rgbMode);
        RGBMode prevMode = DaemonState.rgbMode;
        int prevR = DaemonState.r;
        int prevG = DaemonState.g;
        int prevB = DaemonState.b;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            boolean stateChanged = prevMode != DaemonState.rgbMode || prevR != DaemonState.r || prevG != DaemonState.g || prevB != DaemonState.b;

            if (stateChanged) {
                applyMode(DaemonState.rgbMode);
                prevMode = DaemonState.rgbMode;
                prevR = DaemonState.r;
                prevG = DaemonState.g;
                prevB = DaemonState.b;
            }

            try {
                DaemonState.brightness = Integer.parseInt(Files.readString(Path.of("/sys/devices/platform/hp-wmi/leds/hp::kbd_backlight/brightness")).trim());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        executorService.shutdown();
    }

    public void applyMode(core.RGBMode mode) {
        if (activeMode != null) {                          //Used for killing prevMode thread.
            activeMode.cancel(true);                   //activeMode is only used for killing threads, and as static mode isn't used
            activeMode = null;                            //with threads, static mode and off mode (static but value 0,0,0) are null.
        }

        switch (mode) {
            case RAINBOW:
                activeMode = executorService.submit(new Rainbow());
                break;
            case STATIC:
                Static.staticRGB(DaemonState.r, DaemonState.g, DaemonState.b, DaemonState.brightness);
                break;
            case OFF:
                Static.offRGB();
                break;
        }
    }
}
