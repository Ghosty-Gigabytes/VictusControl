package daemon;

public class DaemonState {

    public enum RGBMode {
        STATIC, RAINBOW, OFF
    }

    public volatile RGBMode rgbMode = RGBMode.RAINBOW;
    public volatile int r = 255;
    public volatile int g = 0;
    public volatile int b = 0;
    public volatile int rgbSpeed = 20;
    public volatile int brightness = 255;


}
