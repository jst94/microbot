package net.runelite.client.plugins.microbot.varlamoreranged;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "VarlamoreRanged",
        description = "Microbot plugin for Varlamore ranged training",
        tags = {"varlamore", "ranged", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class VarlamoreRangedPlugin extends Plugin {
    @Inject
    private VarlamoreRangedConfig config;
    
    @Provides
    VarlamoreRangedConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(VarlamoreRangedConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private VarlamoreRangedOverlay varlamoreRangedOverlay;

    @Inject
    VarlamoreRangedScript varlamoreRangedScript;

    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(varlamoreRangedOverlay);
        }
        varlamoreRangedScript.run(config);
    }

    protected void shutDown() {
        varlamoreRangedScript.shutdown();
        overlayManager.remove(varlamoreRangedOverlay);
    }
}