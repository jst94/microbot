package net.runelite.client.plugins.microbot.JstScurrius;

import net.runelite.client.plugins.microbot.JstScurrius.ScurriusConfig;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2InventoryItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2InventoryInteraction;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScurriusUtils {
    private static final Logger log = LoggerFactory.getLogger(ScurriusUtils.class);
    protected static final Random random = new Random();
    
    @Inject
    private Client client;
    
    @Inject
    private ClientThread clientThread;
    
    @Inject
    private ScurriusConfig config;
    
    private int nextRunEnergy;

    public List<String> getGearNames(String gear) {
        return Arrays.stream(gear.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    public void swapGear(List<String> gearNames) {
        for (String gearName : gearNames) {
            Rs2Inventory.search()
                    .nameContainsInsensitive(gearName)
                    .first()
                    .ifPresent(item -> Rs2InventoryInteraction.useItem(item, "Equip", "Wield", "Wear"));
        }
    }

    public static List<Rs2InventoryItem> nameContainsNoCase(String name) {
        return Rs2Inventory.search()
                .filter(widget -> widget.getName().toLowerCase().contains(name.toLowerCase()))
                .result();
    }

    public void toggleGear(List<String> gearNames) {
        if (this.client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        this.swapGear(gearNames);
    }

    public void sendIntValue(int amount) {
        this.client.setVarcStrValue(359, Integer.toString(amount));
        this.client.setVarcIntValue(5, 7);
        this.client.runScript(681);
    }

    public int tickDelay() {
        return (int) this.randomDelay(
                this.config.tickDelayWeightedDistribution(),
                this.config.tickDelayMin(),
                this.config.tickDelayMax(),
                this.config.tickDelayDeviation(),
                this.config.tickDelayTarget()
        );
    }

    public long randomDelay(boolean weightedDistribution, int min, int max, int deviation, int target) {
        if (weightedDistribution) {
            return (long) this.clamp(
                    -Math.log(Math.abs(random.nextGaussian())) * deviation + target,
                    min,
                    max
            );
        }
        return (long) this.clamp(
                Math.round(random.nextGaussian() * deviation + target),
                min,
                max
        );
    }

    private double clamp(double val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    public int getRandomIntBetweenRange(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public boolean isRunEnabled() {
        return this.client.getVarpValue(173) == 1;
    }

    public void handleRun(int minEnergy, int randMax) {
        if (this.nextRunEnergy < minEnergy || this.nextRunEnergy > minEnergy + randMax) {
            this.nextRunEnergy = this.getRandomIntBetweenRange(
                    minEnergy,
                    minEnergy + this.getRandomIntBetweenRange(0, randMax)
            );
        }
        
        if (!(this.client.getEnergy() / 100 <= this.nextRunEnergy && 
              this.client.getVarbitValue(25) == 0 || 
              this.isRunEnabled())) {
            this.nextRunEnergy = 0;
            Widget runOrb = this.client.getWidget(WidgetInfo.MINIMAP_RUN_ORB);
            if (runOrb != null) {
                // Handle run orb interaction if needed
            }
        }
    }
}
