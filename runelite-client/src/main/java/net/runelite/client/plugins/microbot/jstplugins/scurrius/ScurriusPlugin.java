package net.runelite.client.plugins.microbot.jstplugins.scurrius;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = "Scurrius",
        description = "Kills the Scurrius Boss",
        tags = {"microbot", "scurrius", "boss"},
        enabledByDefault = false
)
@Slf4j
public class ScurriusPlugin extends Plugin {
    @Inject
    private ScurriusConfig config;

    @Provides
    ScurriusConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ScurriusConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ScurriusOverlay overlay;

    @Inject
    private ScurriusScript script;

    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(overlay);
        }
        script.state = ScurriusScript.State.BANKING;
        script.run(config);
    }

    @Override
    protected void shutDown() {
        script.shutdown();
        overlayManager.remove(overlay);
    }
}
