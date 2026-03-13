package cli;

import core.DaemonState;
import org.json.JSONObject;
import java.io.IOException;

public class Main {

    private static SocketClient client;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: victus-ctl <keyboard|fan> <command> [args]");
            return;
        }

        client = new SocketClient();

        try {
            switch (args[0]) {
                case "setKeyboard" -> handleSetKeyboard(args);
                case "setFan"      -> handleSetFan(args);
                case "getKeyboard" -> handleGetKeyboard();
                case "getFan"      -> handleGetFan();
                case "help"        -> printHelp();
                case "about"       -> printAbout();
                default         -> System.out.println("Unknown command. Run 'victus-ctl help'");
            }
        } catch (IOException e) {
            System.err.println("Could not connect to daemon — is it running?");
            System.exit(1);
        }
    }

    private static void printAbout() {
    }

    private static void handleGetFan() throws IOException {
        String fanDataString = client.sendCommand("getFan");
        JSONObject fanData = new JSONObject(fanDataString);

        System.out.println("-Getting Fan info");
        switch ((Integer) fanData.get("pwmMode")){
            case 0:
                System.out.println("|-Current PWM mode: MAX");
                break;
            case 1:
                System.out.println("|-Current PWM mode: MANUAL");
                System.out.println("|-Fan1 Target RPM:" + fanData.get("fan1Target"));
                System.out.println("|-Fan2 Target RPM:" + fanData.get("fan2Target"));
                break;
            case 2:
                System.out.println("|-Current PWM mode: AUTO");
                break;
        }
        System.out.println("|-Fan1 Max RPM:" + fanData.get("fan1Max"));
        System.out.println("|-Fan2 Max RPM:" + fanData.get("fan2Max"));
        System.out.println("|-Fan1 Current RPM:" + fanData.get("fan1Input"));
        System.out.println("|-Fan2 Current RPM:" + fanData.get("fan2Input"));
    }

    private static void handleGetKeyboard() {
        
    }

    private static void handleSetKeyboard(String[] args) throws IOException {
        switch (args[1]) {
            case "rainbow" -> {
                if (args.length == 4){
                    client.sendCommand("setKeyboard rainbow " + args[2] + " " + args[3]);
                }
                else if (args.length == 3){
                    System.out.println("Applying default values");
                    System.out.println("Delay: " + DaemonState.rgbSpeed);
                    client.sendCommand("setKeyboard rainbow " + args[2] + " " + DaemonState.rgbSpeed);
                }
                else{
                    System.out.println("Applying default values");
                    System.out.println("Brightness: " + DaemonState.brightness);
                    System.out.println("Delay: " + DaemonState.rgbSpeed + "ms");
                    client.sendCommand("setKeyboard rainbow " + DaemonState.brightness + " " + DaemonState.rgbSpeed);
                }
            }
            case "off"     -> client.sendCommand("setKeyboard off");
            case "static"  -> {
                if (args.length == 6) {
                    client.sendCommand("setKeyboard static " + args[2] + " " + args[3] + " " + args[4] + " " + args[5]);
                }
                else if(args.length ==5){
                    System.out.println("Applying default values");
                    System.out.println("Brightness: " + DaemonState.brightness);
                    client.sendCommand("setKeyboard static " + args[2] + " " + args[3] + " " + args[4] + " " + DaemonState.brightness);
                }
                else {
                    System.out.println("Usage: victus-ctl keyboard static <r> <g> <b> <brightness>");
                }
            }
            default -> System.out.println("Unknown keyboard command. Run 'victus-ctl help'");
        }
    }

    private static void handleSetFan(String[] args) throws IOException {
        switch (args[1]) {
            case "auto" -> client.sendCommand("setFan auto");
            case "max"  -> client.sendCommand("setFan max");
            case "manual" -> {
                if (args.length == 4) {
                    client.sendCommand("setFan manual " + args[2] + " " + args[3]);
                } else if (args.length == 3) {
                    client.sendCommand("setFan manual " + args[2] + " " + args[2]);
                } else {
                    System.out.println("Applying default values");
                    System.out.println("FAN1: " + DaemonState.fan1_target);
                    System.out.println("FAN2: " + DaemonState.fan2_target);
                    client.sendCommand("setFan manual " + DaemonState.fan2_target + " " + DaemonState.fan2_target);
                }
            }
            default -> System.out.println("Unknown fan command. Run 'victus-ctl help'");
        }
    }

    private static void printHelp() {
        System.out.println("""
                victus-ctl — Victus laptop control CLI
                
                Keyboard:
                  victus-cli keyboard rainbow
                  victus-cli keyboard rainbow <brightness>
                  victus-ctl keyboard rainbow <brightness> <delay>
                  victus-ctl keyboard off
                  victus-ctl keyboard static <r> <g> <b>
                  victus-ctl keyboard static <r> <g> <b> <brightness>
                
                Fan:
                  victus-ctl fan auto
                  victus-ctl fan max
                  victus-ctl fan manual
                  victus-ctl fan manual <rpm>
                  victus-ctl fan manual <fan1_rpm> <fan2_rpm>
                """);
    }
}