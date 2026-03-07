package daemon;

import core.DaemonState;
import daemon.fanControl.fan;
import daemon.keyboardEffects.Keyboard;

import java.util.Scanner;


public class Main {

    public static void main(String[] args) {
        DaemonState state = new DaemonState();
        Thread thread = new Thread(new Keyboard(state));
        Thread thread1 = new Thread(new fan(state));
        thread1.start();
        thread.start();
        Runnable test1 = () -> {
            Scanner scanner = new Scanner(System.in);
            while (true){
                System.out.println("tell Mode");
                String name = scanner.nextLine();
                if (name.equals("max")){
                    state.fanMode = core.FANMode.MAX;
                    System.out.println("mode:" + state.fanMode);
                    continue;
                }
                if (name.equals("auto")){
                    state.fanMode = core.FANMode.AUTO;
                    System.out.println("mode:" + state.fanMode);
                    continue;
                }
                if (name.equals("manual")){
                    state.fanMode = core.FANMode.MANUAL;
                    state.fan1_target = 3500;
                    state.fan2_target = 3500;
                    System.out.println("mode:" + state.fanMode);
                }
            }
        };
        Thread test2 = new Thread(test1, "tester");
        test2.start();
    }

}