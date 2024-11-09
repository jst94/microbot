package net.runelite.client.plugins.microbot.jstplugins.jstscurrius;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = "Jst Scurrius",
        description = "Kills the Scurrius Boss",
        tags = {"microbot", "scurrius", "boss"},
        enabledByDefault = false
)
@Slf4j
public class JstScurriusPlugin extends Plugin {
    @Inject
    private JstScurriusConfig config;

    @Provides
    JstScurriusConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(JstScurriusConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    
    @Inject
    private JstScurriusOverlay overlay;

    @Inject
    private JstScurriusScript script;

    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(overlay);
        }
        script.state = JstScurriusScript.State.BANKING;
        script.run(config);
    }

    @Override
    protected void shutDown() {
        script.shutdown();
        overlayManager.remove(overlay);
    }
}
