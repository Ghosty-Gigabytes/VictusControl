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
                case "setkeyboard" -> handleSetKeyboard(args);
                case "setfan"      -> handleSetFan(args);
                case "getkeyboard" -> handleGetKeyboard();
                case "getfan"      -> handleGetFan();
                case "help"        -> printHelp();
                case "about"       -> printAbout();
                default         -> System.out.println("Unknown command. Run 'victus-ctl help'");
            }
        } catch (IOException e) {
            System.err.println("Could not connect to daemon — is it running?");
            System.exit(1);
        }
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

    private static void handleGetKeyboard() throws IOException {
        String kbDataString = client.sendCommand("getKeyboard");
        JSONObject kbData = new JSONObject(kbDataString);

        System.out.println("-Getting Keyboard info");
        System.out.println("|-Current Mode: " + kbData.get("rgbMode"));
        switch ((String) kbData.get("rgbMode")){
            case "RAINBOW":
                System.out.println("|-Current Brightness: " + kbData.get("brightness"));
                System.out.println("|-Max brightness: " + kbData.get("maxBrightness"));
                System.out.println("|-Delay: " + kbData.get("speed") + "ms");
                break;
            case "STATIC":
                System.out.println("|-Red: " + kbData.get("r"));
                System.out.println("|-Green: " + kbData.get("g"));
                System.out.println("|-Blue: " + kbData.get("b"));
                System.out.println("|-Current Brightness: " + kbData.get("brightness"));
                System.out.println("|-Max brightness: " + kbData.get("maxBrightness"));
        }
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
            case "brightness" -> client.sendCommand("setKeyboard brightness " + args[2]);
            case "delay" -> client.sendCommand("setKeyboard delay " + args[2]);
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
                  victus-cli setkeyboard rainbow
                  victus-cli setkeyboard rainbow <brightness>
                  victus-ctl setkeyboard rainbow <brightness> <delay>
                  victus-ctl setkeyboard off
                  victus-ctl setkeyboard static <r> <g> <b>
                  victus-ctl setkeyboard static <r> <g> <b> <brightness>
                  victus-ctl setkeyboard brightness <brightness>
                  victus-ctl setkeyboard delay <delay>
                  
                  victus-ctl getkeyboard
                
                Fan:
                  victus-ctl setfan auto
                  victus-ctl setfan max
                  victus-ctl setfan manual
                  victus-ctl setfan manual <rpm>
                  victus-ctl setfan manual <fan1_rpm> <fan2_rpm>
                  
                  victus-ctl getfan
                """);
    }

    private static void printAbout() {
        // ANSI color codes
        final String RESET  = "\033[0m";
        final String BOLD   = "\033[1m";
        final String CYAN   = "\033[36m";
        final String BLUE   = "\033[34m";
        final String GREEN  = "\033[32m";
        final String YELLOW = "\033[33m";
        final String DIM    = "\033[2m";

        System.out.println(BLUE + BOLD + """

            ██╗   ██╗██╗ ██████╗████████╗██╗   ██╗███████╗
            ██║   ██║██║██╔════╝╚══██╔══╝██║   ██║██╔════╝
            ██║   ██║██║██║        ██║   ██║   ██║███████╗
            ╚██╗ ██╔╝██║██║        ██║   ██║   ██║╚════██║
             ╚████╔╝ ██║╚██████╗   ██║   ╚██████╔╝███████║
              ╚═══╝  ╚═╝ ╚═════╝   ╚═╝    ╚═════╝ ╚══════╝

       ██████╗ ██████╗ ███╗   ██╗████████╗██████╗  ██████╗ ██╗
      ██╔════╝██╔═══██╗████╗  ██║╚══██╔══╝██╔══██╗██╔═══██╗██║
      ██║     ██║   ██║██╔██╗ ██║   ██║   ██████╔╝██║   ██║██║
      ██║     ██║   ██║██║╚██╗██║   ██║   ██╔══██╗██║   ██║██║
      ╚██████╗╚██████╔╝██║ ╚████║   ██║   ██║  ██║╚██████╔╝███████╗
       ╚═════╝ ╚═════╝ ╚═╝  ╚═══╝   ╚═╝   ╚═╝  ╚═╝ ╚═════╝ ╚══════╝
    """ + RESET);
        System.out.println();
        System.out.println(DIM   + "        ┌─────────────────────────────────────────────┐" + RESET);
        System.out.println(RESET + "        │  " + YELLOW + "Version  " + RESET + ": " + GREEN + BOLD + "1.0.0" + RESET + "                           │");
        System.out.println(RESET + "        │  " + YELLOW + "Author   " + RESET + ": ghosty-gigabytes                │");
        System.out.println(RESET + "        │  " + YELLOW + "License  " + RESET + ": GPL-3.0                         │");
        System.out.println(RESET + "        │  " + YELLOW + "GitHub   " + RESET + ": github.com/ghosty-gigabytes/    │");
        System.out.println(RESET + "        │             VictusControl                   │");
        System.out.println(DIM   + "        ├─────────────────────────────────────────────┤" + RESET);
        System.out.println(RESET + "        │  Keyboard backlight and fan control         │");
        System.out.println(RESET + "        │  daemon for HP Victus laptops on Linux.     │");
        System.out.println(RESET + "        │                                             │");
        System.out.println(RESET + "        │  Run " + CYAN + "'victus-ctl help'" + RESET + " for usage.           │");
        System.out.println(DIM   + "        └─────────────────────────────────────────────┘" + RESET);
        System.out.println();
    }

}