package daemon.fanControl;

import core.DaemonState;
import core.FANMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Fan implements Runnable {
    private final DaemonState state;
    private Future<?> constMode;
    private final ExecutorService service = Executors.newSingleThreadExecutor();


    public Fan(DaemonState state) {
        this.state = state;
    }

    @Override
    public void run() {
        applyMode(state.fanMode);

        FANMode prevMode = state.fanMode;
        int prevFan1 = state.fan1_target;
        int prevFan2 = state.fan2_target;


        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(100);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            if (prevMode != state.fanMode || prevFan1 != state.fan1_target || prevFan2 != state.fan2_target) {
                applyMode(state.fanMode);
                prevMode = state.fanMode;
                prevFan1 = state.fan1_target;
                prevFan2 = state.fan2_target;
            }
        }

        service.shutdown();
    }

    private void applyMode(core.FANMode fanMode) {
        if (constMode != null) {
            constMode.cancel(true);
            constMode = null;
        }
        switch (fanMode) {
            case core.FANMode.MAX:
                try {
                    Files.writeString(Path.of(state.hwmonPath + "/pwm1_enable"), "0");
                } catch (IOException e) {
                    throw new RuntimeException("pwm1_enable file not found", e);
                }
                break;
            case core.FANMode.AUTO:
                try {
                    Files.writeString(Path.of(state.hwmonPath + "/pwm1_enable"), "2");
                } catch (IOException e) {
                    throw new RuntimeException("pwm1_enable file not found" + state.hwmonPath + "/pwm1_enable", e);
                }
                break;
            case core.FANMode.MANUAL:
                try {
                    Files.writeString(Path.of(state.hwmonPath + "/pwm1_enable"), "1");
                } catch (IOException e) {
                    throw new RuntimeException("pwm1_enable file not found" + state.hwmonPath + "/pwm1_enable", e);
                }
                constMode = service.submit(new ManualMode(state));
                break;
        }
    }
}
