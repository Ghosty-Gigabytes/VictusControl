package daemon;

import core.DaemonState;
import core.FANMode;
import core.RGBMode;
import org.json.JSONObject;

import java.io.*;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class SocketListener implements Runnable {

    @Override
    public void run() {
        try {
            startServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void startServer() throws IOException {
        Path socketPath = Path.of("/run/victus-control/victus.sock");
        Files.deleteIfExists(socketPath);
        new File("/run/victus-control").mkdir();

        UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(socketPath);

        try (ServerSocketChannel channel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)) {
            channel.bind(socketAddress);
            System.out.println("Socket listening at /run/victus-control/victus.sock");
            while (!Thread.currentThread().isInterrupted()) {
                try (SocketChannel client = channel.accept()) {
                    handleClient(client);
                }

            }
        }
    }

    private void handleClient(SocketChannel client) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(Channels.newInputStream(client)));
        String line = reader.readLine();
        if (line == null || line.isBlank()) return;
        System.out.println(line);
        String[] parts = line.trim().split(" ");
        if (parts.length == 0) {
            System.err.println("Invalid command: " + line);
            return;
        }


        switch (parts[0]) {
            case "setKeyboard":
                handleSetRGB(parts[1], parts);
                break;
            case "setFan":
                handleSetFan(parts[1], parts);
                break;
            case "getFan":
                handleGetFan(client);
                break;
            case "getKeyboard":
                handleGetRGB(client);
                break;
        }
    }

    private void handleGetRGB(SocketChannel client) {
        JSONObject keyboardData = new JSONObject();
        keyboardData.put("rgbMode", DaemonState.rgbMode);
        if (DaemonState.rgbMode.equals(RGBMode.RAINBOW)){
            keyboardData.put("brightness", DaemonState.brightness);
            keyboardData.put("maxBrightness", DaemonState.maxBrightness);
            keyboardData.put("speed", DaemonState.rgbSpeed);
        }
        else if (DaemonState.rgbMode.equals(RGBMode.STATIC)){
            keyboardData.put("r", DaemonState.r);
            keyboardData.put("g", DaemonState.g);
            keyboardData.put("b", DaemonState.b);
            keyboardData.put("brightness", DaemonState.brightness);
            keyboardData.put("maxBrightness", DaemonState.maxBrightness);
        }

        PrintWriter writer = new PrintWriter(Channels.newOutputStream(client), true);
        writer.println(keyboardData);
    }

    private void handleGetFan(SocketChannel client) {
        JSONObject fanData = new JSONObject();
        fanData.put("pwmMode", DaemonState.pwmMode);
        if (DaemonState.pwmMode == 1){
            fanData.put("fan1Target", DaemonState.fan1_target);
            fanData.put("fan2Target", DaemonState.fan2_target);
        }
        fanData.put("fan1Max", DaemonState.fan1_max);
        fanData.put("fan2Max", DaemonState.fan2_max);
        try {
            fanData.put("fan2Input", Integer.parseInt(Files.readString(DaemonState.hwmonPath.resolve("fan1_input")).trim()));
            fanData.put("fan1Input", Integer.parseInt(Files.readString(DaemonState.hwmonPath.resolve("fan2_input")).trim()));
        } catch (IOException e) {
            throw new RuntimeException("Cannot read current RPM state.", e);
        }

        PrintWriter writer = new PrintWriter(Channels.newOutputStream(client), true);
        writer.println(fanData);
    }

    private void handleSetRGB(String command, String[] parts) {
        switch (command) {
            case "off":
                DaemonState.rgbMode = RGBMode.OFF;
                break;
            case "rainbow":
                DaemonState.rgbMode = RGBMode.OFF;
                DaemonState.brightness = Integer.parseInt(parts[2]);
                DaemonState.rgbSpeed = Integer.parseInt(parts[3]);
                DaemonState.rgbMode = RGBMode.RAINBOW;
                break;
            case "brightness":
                DaemonState.brightness = Integer.parseInt(parts[2]);
                try {
                    Files.writeString(Path.of("/sys/devices/platform/hp-wmi/leds/hp::kbd_backlight/brightness"), String.valueOf(Integer.parseInt(parts[2])));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "static":
                DaemonState.rgbMode = RGBMode.OFF;
                DaemonState.r = Integer.parseInt(parts[2]);
                DaemonState.g = Integer.parseInt(parts[3]);
                DaemonState.b = Integer.parseInt(parts[4]);
                DaemonState.brightness = Integer.parseInt(parts[5]);
                DaemonState.rgbMode = RGBMode.STATIC;
                break;
            case "delay":
                DaemonState.rgbSpeed = Integer.parseInt(parts[2]);
        }
    }

    private void handleSetFan(String command, String[] parts) {
        switch (command) {
            case "max":
                DaemonState.fanMode = FANMode.MAX;
                break;
            case "auto":
                DaemonState.fanMode = FANMode.AUTO;
                break;
            case "manual":
                DaemonState.fan1_target = Integer.parseInt(parts[2]);
                DaemonState.fan2_target = Integer.parseInt(parts[3]);
                DaemonState.fanMode = FANMode.MANUAL;
                break;

        }
    }

}

