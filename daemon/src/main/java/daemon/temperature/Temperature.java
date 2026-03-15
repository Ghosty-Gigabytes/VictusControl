package daemon.temperature;

import core.DaemonState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Temperature implements Runnable{


    @SuppressWarnings("BusyWait")
    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted()){
            getTemp();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void getTemp() {
        try {
            DaemonState.k10temp = Float.valueOf((Files.readString(Path.of(DaemonState.k10tempPath + "/temp1_input"))));
        } catch (IOException e) {
            throw new RuntimeException("Cannot access AMD CPU temperature.");
        }
    }
}
