package cli;

import com.sun.security.jgss.GSSUtil;
import core.DaemonState;

import java.io.IOException;

public class Main {
    private static final String SOCKET_PATH = "/run/victus-control/victus.sock";
    private static SocketClient client;
    private static DaemonState state;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: victus-ctl <keyboard|fan> <command> [args]");
            return;
        }

        client = new SocketClient();
        state = new DaemonState();

        try {
            switch (args[0]) {
                case "keyboard" -> handleKeyboard(args);
                case "fan"      -> handleFan(args);
                case "help"     -> printHelp();
                default         -> System.out.println("Unknown command. Run 'victus-ctl help'");
            }
        } catch (IOException e) {
            System.err.println("Could not connect to daemon — is it running?");
            System.exit(1);
        }
    }

    private static void handleKeyboard(String[] args) throws IOException {
        switch (args[1]) {
            case "rainbow" -> {
                if (args.length == 4){
                    client.sendCommand("keyboard rainbow " + args[2] + " " + args[3]);
                }
                else if (args.length == 3){
                    System.out.println("Applying default values");
                    System.out.println("Delay: " + state.rgbSpeed);
                    client.sendCommand("keyboard rainbow " + args[2] + " " + state.rgbSpeed);
                }
                else{
                    System.out.println("Applying default values");
                    System.out.println("Brightness: " + state.brightness);
                    System.out.println("Delay: " + state.rgbSpeed + "ms");
                    client.sendCommand("keyboard rainbow " + state.brightness + " " + state.rgbSpeed);
                }
            }
            case "off"     -> client.sendCommand("keyboard off");
            case "static"  -> {
                if (args.length == 6) {
                    client.sendCommand("keyboard static " + args[2] + " " + args[3] + " " + args[4] + " " + args[5]);
                }
                else if(args.length ==5){
                    System.out.println("Applying default values");
                    System.out.println("Brightness: " + state.brightness);
                    client.sendCommand("keyboard static " + args[2] + " " + args[3] + " " + args[4] + " " + state.brightness);
                }
                else {
                    System.out.println("Usage: victus-ctl keyboard static <r> <g> <b> <brightness>");
                }
            }
            default -> System.out.println("Unknown keyboard command. Run 'victus-ctl help'");
        }
    }

    private static void handleFan(String[] args) throws IOException {
        switch (args[1]) {
            case "auto" -> client.sendCommand("fan auto");
            case "max"  -> client.sendCommand("fan max");
            case "manual" -> {
                if (args.length == 4) {
                    client.sendCommand("fan manual " + args[2] + " " + args[3]);
                } else if (args.length == 3) {
                    client.sendCommand("fan manual " + args[2] + " " + args[2]);
                } else {
                    System.out.println("Applying default values");
                    System.out.println("FAN1: " + state.fan1_target);
                    System.out.println("FAN2: " + state.fan2_target);
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
                  victus-ctl fan manual <rpm>
                  victus-ctl fan manual <fan1_rpm> <fan2_rpm>
                """);
    }
}