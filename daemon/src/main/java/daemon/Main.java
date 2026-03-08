package daemon;


import core.DaemonState;
import daemon.fanControl.Fan;
import daemon.keyboardEffects.Keyboard;

public class Main {

    public static void main(String[] args) {
        DaemonState state = new DaemonState();
        Thread fanThread = new Thread(new Fan(state), "Fan Thread");
        Thread keyboardThread = new Thread(new Keyboard(state), "Keyboard Thread");
        Thread socketThread = new Thread(new SocketListener(state), "SocketListener Thread");

        fanThread.start();
        keyboardThread.start();
        socketThread.start();

    }

}