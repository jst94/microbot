package net.runelite.client.plugins.microbot.jstvale;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.JST + "Vale Totems",
        description = "Automates the Vale Totems minigame for Construction and Fletching XP",
        tags = {"microbot", "vale", "totems", "construction", "fletching", "skilling"},
        enabledByDefault = false
)
@Slf4j
public class JstValePlugin extends Plugin {
    
    @Inject
    private JstValeConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private JstValeOverlay overlay;

    @Inject
    JstValeScript script;

    // Statistics tracking
    private int totemsCompleted = 0;
    private int runsCompleted = 0;
    private int offeringsCollected = 0;
    private long startTime = 0;

    @Provides
    JstValeConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(JstValeConfig.class);
    }

    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(overlay);
        }
        script.run(config, this);
        startTime = System.currentTimeMillis();
        resetStatistics();
    }

    protected void shutDown() {
        script.shutdown();
        overlayManager.remove(overlay);
        resetStatistics();
    }

    @Subscribe
    public void onGameTick(GameTick gameTick) {
        // Update statistics or handle game tick events if needed
    }

    // Statistics methods
    public void incrementTotemsCompleted() {
        totemsCompleted++;
    }

    public void incrementRunsCompleted() {
        runsCompleted++;
    }

    public void incrementOfferingsCollected() {
        offeringsCollected++;
    }

    public int getTotemsCompleted() {
        return totemsCompleted;
    }

    public int getRunsCompleted() {
        return runsCompleted;
    }

    public int getOfferingsCollected() {
        return offeringsCollected;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getTotemsPerHour() {
        long runtime = System.currentTimeMillis() - startTime;
        if (runtime > 0) {
            return (int) ((totemsCompleted * 3600000.0) / runtime);
        }
        return 0;
    }

    public int getRunsPerHour() {
        long runtime = System.currentTimeMillis() - startTime;
        if (runtime > 0) {
            return (int) ((runsCompleted * 3600000.0) / runtime);
        }
        return 0;
    }

    private void resetStatistics() {
        totemsCompleted = 0;
        runsCompleted = 0;
        offeringsCollected = 0;
    }
}