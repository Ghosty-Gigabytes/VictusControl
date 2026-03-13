package core;

import java.nio.file.Path;

public class DaemonState {

    public static volatile Path hwmonPath;

    public static volatile FANMode fanMode = FANMode.AUTO;
    public static volatile int fan1_max;
    public static volatile int fan1_target = 2500;
    public static volatile int fan1_input;
    public static volatile int fan2_max;
    public static volatile int fan2_target = 2500;
    public static volatile int fan2_input;
    public static volatile int pwmMode;

    public static volatile RGBMode rgbMode = RGBMode.RAINBOW;
    public static volatile int r = 255;
    public static volatile int g = 0;
    public static volatile int b = 0;
    public static volatile int rgbSpeed = 20;
    public static volatile int brightness = 255;


}
