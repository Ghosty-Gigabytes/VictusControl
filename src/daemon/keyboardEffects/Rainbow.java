package daemon.keyboardEffects;
import core.DaemonState;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Rainbow implements Runnable{

    private final DaemonState state;

    @Override
    public void run() {
        rainbowEffect();
    }

    public Rainbow(DaemonState state){
        this.state = state;

    }

    private void rainbowEffect() {
        Path brightnessPath = Path.of("/sys/class/leds/hp::kbd_backlight/brightness");
        Path colorPath = Path.of("/sys/class/leds/hp::kbd_backlight/multi_intensity");

        try {
            Files.writeString(brightnessPath, String.valueOf(state.brightness));
        } catch (IOException e) {
            throw new RuntimeException("Failed to set brightness", e);
        }

        int h = 0;
        while (!Thread.currentThread().isInterrupted()){
            int[] rgb = hsvToRgb(h%360);
            try {
                Files.writeString(colorPath, rgb[0] + " " +  rgb[1] + " " + rgb[2]);
                h+=1;
                Thread.sleep(state.rgbSpeed);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write color values", e);
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
                break;
            }
        }

    }

    private int[] hsvToRgb(int h){
        int sector = h / 60;
        double f = ((double) h / 60) - sector;
        double q = 1 - f;
        double r=0, g=0, b =0;

        switch (sector) {
            case 0: r=1; g=f; b=0; break;
            case 1: r=q; g=1; b=0; break;
            case 2: r=0; g=1; b=f; break;
            case 3: r=0; g=q; b=1; break;
            case 4: r=f; g=0; b=1; break;
            case 5: r=1; g=0; b=q; break;
        }

        return new int[]{
                (int) Math.round(r*255), (int) Math.round(g*255), (int)Math.round(b*255)
        };
    }
}
