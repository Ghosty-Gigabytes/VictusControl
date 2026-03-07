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

        while(!Thread.currentThread().isInterrupted()){
            try{
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            if (prevMode != state.rgbMode){
                applyMode(state.rgbMode);
                prevMode = state.rgbMode;
            }
        }

        executorService.shutdown();
    }

    public void applyMode(core.RGBMode mode) {
         if (activeMode != null){                          //Used for killing prevMode thread.
             activeMode.cancel(true);                   //activeMode is only used for killing threads, and as static mode isnt used
             activeMode = null;                            //with threads, static mode and off mode (static but value 0,0,0) are null.
         }

         switch(mode){
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
