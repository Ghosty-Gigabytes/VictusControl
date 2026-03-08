package daemon.keyboardEffects;

import core.DaemonState;
import core.RGBMode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Keyboard implements Runnable {

    private final DaemonState state;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Future<?> activeMode;


    public Keyboard(DaemonState state) {
        this.state = state;
    }

    @Override
    public void run() {
        applyMode(state.rgbMode);
        RGBMode prevMode = state.rgbMode;
        int prevR = state.r;
        int prevG = state.g;
        int prevB = state.b;
        int prevBrightness = state.brightness;
        int prevSpeed = state.rgbSpeed;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            boolean stateChanged = prevMode != state.rgbMode || prevBrightness != state.brightness || prevSpeed != state.rgbSpeed | prevR != state.r || prevG != state.g || prevB != state.b;

            if (stateChanged) {
                applyMode(state.rgbMode);
                prevMode = state.rgbMode;
                prevR = state.r;
                prevG = state.g;
                prevB = state.b;
                prevBrightness = state.brightness;
                prevSpeed = state.rgbSpeed;
            }
        }

        executorService.shutdown();
    }

    public void applyMode(core.RGBMode mode) {
        if (activeMode != null) {                          //Used for killing prevMode thread.
            activeMode.cancel(true);                   //activeMode is only used for killing threads, and as static mode isnt used
            activeMode = null;                            //with threads, static mode and off mode (static but value 0,0,0) are null.
        }

        switch (mode) {
            case RAINBOW:
                activeMode = executorService.submit(new Rainbow(state));
                break;
            case STATIC:
                Static.staticRGB(state.r, state.g, state.b, state.brightness);
                break;
            case OFF:
                Static.offRGB();
                break;
        }
    }
}
