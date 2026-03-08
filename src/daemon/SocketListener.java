package daemon;

import core.DaemonState;
import core.FANMode;
import core.RGBMode;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class SocketListener implements Runnable {

    private DaemonState state;

    public SocketListener(DaemonState state){
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

            while(!Thread.currentThread().isInterrupted()){
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

        String[] parts = line.trim().split(" ");
        if (parts.length < 2) {
            System.err.println("Invalid command: " + line);
            return;
        }

        String catagory = parts[0];
        String command = parts[1];

        switch (catagory){
            case "keyboard":
                handleRGB(command, parts);
                break;
            case "fan":
                handleFan(command, parts);
                break;
        }

    }

    private void handleRGB(String command, String[] parts) {
        switch (command){
            case "off":
                state.rgbMode = RGBMode.OFF;
                break;
            case "rainbow":
                state.rgbMode = RGBMode.RAINBOW;
                state.brightness = Integer.parseInt(parts[2]);
                state.rgbSpeed = Integer.parseInt(parts[3]);
                break;
            case "brightness":
                state.brightness = Integer.parseInt(parts[2]);
                break;
            case "static":
                state.r = Integer.parseInt(parts[2]);
                state.g = Integer.parseInt(parts[3]);
                state.b = Integer.parseInt(parts[4]);
                state.brightness = Integer.parseInt(parts[5]);
                state.rgbMode = RGBMode.STATIC;
                break;
        }
    }

    private void handleFan(String command, String[] parts) {
        switch (command){
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

