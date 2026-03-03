
import java.io.FileWriter;
import java.io.IOException;



public class Main {
    static String colorPath = "/sys/class/leds/hp::kbd_backlight/multi_intensity";
    static String brightnessPath = "/sys/class/leds/hp::kbd_backlight/brightness";
    static int delay = 20;
    public static void main(String[] args) throws InterruptedException {
//        ProcessBuilder pb = new ProcessBuilder();

//        while (true){
//            r = 255;
//            while (g <256){
//                String color = "echo " + r  + " " + g + " " + b + " ";
//                String cmd =  color + "> /sys/class/leds/hp::kbd_backlight/multi_intensity";
//                pb.command("sudo", "sh", "-c", cmd);
//                pb.inheritIO(); // shows sudo prompt
//                Process p = pb.start();
//                p.waitFor();
//                g+=18;
//                Thread.sleep(20);
//            }
//            g=255;
//            while (r >=0){
//                String color = "echo " + r  + " " + g + " " + b + " ";
//                String cmd =  color + "> /sys/class/leds/hp::kbd_backlight/multi_intensity";
//                pb.command("sudo", "sh", "-c", cmd);
//                pb.inheritIO(); // shows sudo prompt
//                Process p = pb.start();
//                p.waitFor();
//                r-=18;
//                Thread.sleep(20);
//            }
//            r=0;
//            while (b < 256){
//                String color = "echo " + r  + " " + g + " " + b + " ";
//                String cmd =  color + "> /sys/class/leds/hp::kbd_backlight/multi_intensity";
//                pb.command("sudo", "sh", "-c", cmd);
//                pb.inheritIO(); // shows sudo prompt
//                Process p = pb.start();
//                p.waitFor();
//                b+=18;
//                Thread.sleep(20);
//            }
//            b=255;
//            while(g>=0){
//                String color = "echo " + r  + " " + g + " " + b + " ";
//                String cmd =  color + "> /sys/class/leds/hp::kbd_backlight/multi_intensity";
//                pb.command("sudo", "sh", "-c", cmd);
//                pb.inheritIO(); // shows sudo prompt
//                Process p = pb.start();
//                p.waitFor();
//                g-=18;
//                Thread.sleep(20);
//            }
//            g=0;
//            while (r < 256){
//                String color = "echo " + r  + " " + g + " " + b + " ";
//                String cmd =  color + "> /sys/class/leds/hp::kbd_backlight/multi_intensity";
//                pb.command("sudo", "sh", "-c", cmd);
//                pb.inheritIO(); // shows sudo prompt
//                Process p = pb.start();
//                p.waitFor();
//                Thread.sleep(20);
//                r+=18;
//            }
//            r=255;
//            while (b >=0){
//                String color = "echo " + r  + " " + g + " " + b + " ";
//                String cmd =  color + "> /sys/class/leds/hp::kbd_backlight/multi_intensity";
//                pb.command("sudo", "sh", "-c", cmd);
//                pb.inheritIO(); // shows sudo prompt
//                Process p = pb.start();
//                p.waitFor();
//                b-=18;
//                Thread.sleep(20);
//
//            }
//            b=0;
//        }
        try (FileWriter writer = new FileWriter(brightnessPath)){
            writer.write(255); // Fedora reduces brightness to 10 after every boot;
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int h = 0;
        try (FileWriter writer = new FileWriter(colorPath)) {
            while (true){
                h=h%360;
                int[] rgb = hsvToRgb(h);
                writer.write(rgb[0] + " " +  rgb[1] + " " + rgb[2]);
                writer.flush();
                h+=1;
                Thread.sleep(delay);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static int[] hsvToRgb(int h){
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