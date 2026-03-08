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
    private Path hwmonPath = Path.of("/sys/devices/platform/hp-wmi/hwmon");
    private final DaemonState state;
    private Future<?> constMode;
    private final ExecutorService service = Executors.newSingleThreadExecutor();


    public Fan(DaemonState state) {
        this.state = state;
    }

    @Override
    public void run() {
        hwmonPath = getFullPath();
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

    private Path getFullPath() {
        try (var entries = Files.walk(hwmonPath)) {
            return entries
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("hwmon"))
                    .filter(path -> path.resolve("pwm1_enable").toFile().exists())
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("HP hwmon directory not found"));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void applyMode(core.FANMode fanMode) {
        if (constMode != null) {
            constMode.cancel(true);
            constMode = null;
        }
        switch (fanMode) {
            case core.FANMode.MAX:
                try {
                    Files.writeString(Path.of(hwmonPath + "/pwm1_enable"), "0");
                } catch (IOException e) {
                    throw new RuntimeException("pwm1_enable file not found", e);
                }
                break;
            case core.FANMode.AUTO:
                try {
                    Files.writeString(Path.of(hwmonPath + "/pwm1_enable"), "2");
                } catch (IOException e) {
                    throw new RuntimeException("pwm1_enable file not found" + hwmonPath + "/pwm1_enable", e);
                }
                break;
            case core.FANMode.MANUAL:
                constMode = service.submit(new ManualMode(state, hwmonPath));
                break;
        }
    }
}
