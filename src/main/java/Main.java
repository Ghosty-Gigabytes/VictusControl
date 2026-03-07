import Daemon.DaemonState;
import keyboardEffects.Keyboard;

import java.util.Scanner;


public class Main {

    public static void main(String[] args) throws InterruptedException {
        DaemonState state = new DaemonState();
        Thread thread = new Thread(new Keyboard(state));
        thread.start();
        Runnable test1 = () -> {
            Scanner scanner = new Scanner(System.in);
            while (true){
                System.out.println("tell Mode");
                String name = scanner.nextLine();
                if (name.equals("rainbow")){
                    state.rgbMode = DaemonState.RGBMode.RAINBOW;
                    System.out.println("mode:" + state.rgbMode);
                    continue;
                }
                if (name.equals("static")){
                    state.rgbMode = DaemonState.RGBMode.STATIC;
                    System.out.println("mode:" + state.rgbMode);
                    continue;
                }
                if (name.equals("off")){
                    state.rgbMode = DaemonState.RGBMode.OFF;
                    System.out.println("mode:" + state.rgbMode);
                }
            }
        };
        Thread test2 = new Thread(test1, "tester");
        test2.start();
    }

}