package daemon;

public class DaemonState {

    public enum RGBMode {
        STATIC, RAINBOW, OFF
    }

    public enum FANMode {
        MAX, MANUAL, AUTO
    }

    public volatile FANMode fanMode = FANMode.AUTO;
    public volatile int fan1_max;
    public volatile int fan1_target;
    public volatile int fan1_input;
    public volatile int fan2_max;
    public volatile int fan2_target;
    public volatile int fan2_input;


    public volatile RGBMode rgbMode = RGBMode.RAINBOW;
    public volatile int r = 255;
    public volatile int g = 0;
    public volatile int b = 0;
    public volatile int rgbSpeed = 20;
    public volatile int brightness = 255;


}
