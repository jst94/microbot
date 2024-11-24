package net.runelite.client.plugins.microbot.AutoBankSkillerPlugin;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.api.events.GameTick;

@Slf4j
@PluginDescriptor(
        name = "Auto Bank Skiller",
        description = "Automatically banks and skills",
        tags = {"microbot", "skilling", "banking"}
)
public class AutoBankSkillerPlugin extends Plugin {
    @Inject
    private AutoBankSkillerConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private AutoBankSkillerOverlay overlay;
    private AutoBankSkillerScript script;

    @Provides
    AutoBankSkillerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoBankSkillerConfig.class);
    }

    @Provides
    AutoBankSkillerScript provideScript() {
        if (script == null) {
            script = new AutoBankSkillerScript(config);
        }
        return script;
    }

    @Override
    protected void startUp() {
        Microbot.pauseAllScripts = false;
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        script = null;
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (script == null || Microbot.pauseAllScripts) return;
        
        try {
            script.run();
        } catch (Exception e) {
            log.error("Error during script execution", e);
        }
    }
}