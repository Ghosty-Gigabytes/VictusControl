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

    private DaemonState state;

    public SocketListener(DaemonState state) {
        this.state = state;
    }

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
                state.rgbMode = RGBMode.OFF;
                break;
            case "rainbow":
                state.rgbMode = RGBMode.OFF;
                state.brightness = Integer.parseInt(parts[2]);
                state.rgbSpeed = Integer.parseInt(parts[3]);
                state.rgbMode = RGBMode.RAINBOW;
                break;
            case "brightness":
                state.brightness = Integer.parseInt(parts[2]);
                break;
            case "static":
                state.rgbMode = RGBMode.OFF;
                state.r = Integer.parseInt(parts[2]);
                state.g = Integer.parseInt(parts[3]);
                state.b = Integer.parseInt(parts[4]);
                state.brightness = Integer.parseInt(parts[5]);
                state.rgbMode = RGBMode.STATIC;
                break;
        }
    }

    private void handleSetFan(String command, String[] parts) {
        switch (command) {
            case "max":
                state.fanMode = FANMode.MAX;
                break;
            case "auto":
                state.fanMode = FANMode.AUTO;
                break;
            case "manual":
                state.fan1_target = Integer.parseInt(parts[2]);
                state.fan2_target = Integer.parseInt(parts[3]);
                state.fanMode = FANMode.MANUAL;
                break;

        }
    }

}

