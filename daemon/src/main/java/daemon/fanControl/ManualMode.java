package daemon.fanControl;

import core.DaemonState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ManualMode implements Runnable {
    @Override
    @SuppressWarnings("BusyWait")
    public void run() {
        setFanSpeed();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(100_000);
                if (!(getFanSpeed(1) == DaemonState.fan1_target && getFanSpeed(2) == DaemonState.fan2_target)) {
                    setFanSpeed();
                }

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    @SuppressWarnings("BusyWait")
    private void setFanSpeed() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Files.writeString(Path.of(DaemonState.hwmonPath + "/fan1_target"), String.valueOf(DaemonState.fan1_target));
                Files.writeString(Path.of(DaemonState.hwmonPath + "/fan2_target"), String.valueOf(DaemonState.fan2_target));
            } catch (IOException e) {
                throw new RuntimeException("Target files not found", e);
            }
            if (getFanSpeed(1) == DaemonState.fan1_target && getFanSpeed(2) == DaemonState.fan2_target) {
                break;
            } else {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private int getFanSpeed(int fan) {
        try {
            return Integer.parseInt(Files.readString(Path.of(DaemonState.hwmonPath + "/fan" + fan + "_input")));
        } catch (IOException e) {
            throw new RuntimeException("fan" + fan + "_input file not found", e);
        }
    }

}
