package net.runelite.client.plugins.microbot.jstplugins.jstscurrius.JstScurriusScript;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.jstplugins.jstscurrius.JstScurriusScript.State;
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
    private JstScurriusOverlay exampleOverlay;

    @Inject
    JstScurriusScript scurriusScript;


    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(exampleOverlay);
        }
        JstScurriusScript.state = State.BANKING;
        scurriusScript.run((JstScurriusConfig) config);
    }

    @Override
    protected void shutDown() {
        scurriusScript.shutdown();
        overlayManager.remove(exampleOverlay);
    }
}
