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
    private Future<?> constMode;
    private final ExecutorService service = Executors.newSingleThreadExecutor();



    @Override
    public void run() {
        applyMode(DaemonState.fanMode);

        FANMode prevMode = DaemonState.fanMode;
        int prevFan1 = DaemonState.fan1_target;
        int prevFan2 = DaemonState.fan2_target;


        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(100);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            if (prevMode != DaemonState.fanMode || prevFan1 != DaemonState.fan1_target || prevFan2 != DaemonState.fan2_target) {
                applyMode(DaemonState.fanMode);
                prevMode = DaemonState.fanMode;
                prevFan1 = DaemonState.fan1_target;
                prevFan2 = DaemonState.fan2_target;
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
                    Files.writeString(Path.of(DaemonState.hwmonPath + "/pwm1_enable"), "0");
                } catch (IOException e) {
                    throw new RuntimeException("pwm1_enable file not found", e);
                }
                break;
            case core.FANMode.AUTO:
                try {
                    Files.writeString(Path.of(DaemonState.hwmonPath + "/pwm1_enable"), "2");
                } catch (IOException e) {
                    throw new RuntimeException("pwm1_enable file not found" + DaemonState.hwmonPath + "/pwm1_enable", e);
                }
                break;
            case core.FANMode.MANUAL:
                try {
                    Files.writeString(Path.of(DaemonState.hwmonPath + "/pwm1_enable"), "1");
                } catch (IOException e) {
                    throw new RuntimeException("pwm1_enable file not found" + DaemonState.hwmonPath + "/pwm1_enable", e);
                }
                constMode = service.submit(new ManualMode());
                break;
        }
    }
}
