package net.runelite.client.plugins.microbot.util;

import java.util.Random;

public class Utility {
    private static final Random random = new Random();

    public static void sleepGaussian(int min, int max) {
        try {
            double gaussian = random.nextGaussian();
            gaussian = Math.max(-3, Math.min(3, gaussian));
            
            double normalized = (gaussian + 3) / 6;  // Normalize to 0-1 range
            int sleep = (int) (min + normalized * (max - min));
            
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
