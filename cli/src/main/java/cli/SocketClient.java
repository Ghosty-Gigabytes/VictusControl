package cli;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;

public class SocketClient{


    public SocketClient(){

    }

    public String sendCommand(String command) throws IOException {
        Path socketPath = Path.of("/run/victus-control/victus.sock");
        UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(socketPath);

        SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
        channel.connect(socketAddress);

        PrintWriter out = new PrintWriter(Channels.newOutputStream(channel), true);
        out.println(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(Channels.newInputStream(channel)));
        channel.shutdownOutput();
        return reader.readLine();

    }
}
