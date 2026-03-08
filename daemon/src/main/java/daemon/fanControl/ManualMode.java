package daemon.fanControl;

import core.DaemonState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ManualMode implements Runnable{
    private final DaemonState state;
    private final Path path;

    public ManualMode(DaemonState state, Path path){
        this.state = state;
        this.path = path;
    }

    @Override
    public void run() {
        setFanSpeed();
        while (!Thread.currentThread().isInterrupted()){
            try {
                Thread.sleep(100_000);
                if (getFanSpeed(1) == state.fan1_target && getFanSpeed(2) == state.fan2_target){

                }
                else{
                    setFanSpeed();
                }

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void setFanSpeed(){
        while (!Thread.currentThread().isInterrupted()){
            try {
                Files.writeString(Path.of(path + "/fan1_target"), String.valueOf(state.fan1_target));
                Files.writeString(Path.of(path + "/fan2_target"), String.valueOf(state.fan2_target));
            } catch (IOException e) {
                throw new RuntimeException("Target files not found", e);
            }
            if (getFanSpeed(1) == state.fan1_target && getFanSpeed(2) == state.fan2_target){
                break;
            }
            else{
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
            return Integer.parseInt(Files.readString(Path.of(path + "/fan" + fan + "_input")));
        } catch (IOException e) {
            throw new RuntimeException("fan"+fan+"_input file not found", e);
        }
    }

}
