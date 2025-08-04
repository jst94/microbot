package net.runelite.client.plugins.microbot.jstwc;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.JST + "JstWC",
        description = "2-tick woodcutting teaks on Isle of Souls using birds for aggression",
        tags = {"microbot", "woodcutting", "2-tick", "teaks", "skilling"},
        enabledByDefault = false
)
@Slf4j
public class JstWCPlugin extends Plugin {
    
    @Inject
    private JstWCConfig config;
    
    @Inject
    private OverlayManager overlayManager;
    
    @Inject
    private JstWCOverlay overlay;
    
    @Inject
    private JstWCScript script;
    
    private long startTime;
    private int logsGained = 0;
    private int initialWoodcuttingXp = 0;
    
    @Provides
    JstWCConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(JstWCConfig.class);
    }
    
    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(overlay);
        }
        
        startTime = System.currentTimeMillis();
        if (Microbot.getClient().getSkillExperience(net.runelite.api.Skill.WOODCUTTING) > 0) {
            initialWoodcuttingXp = Microbot.getClient().getSkillExperience(net.runelite.api.Skill.WOODCUTTING);
        }
        
        script.run(config, this);
    }
    
    @Override
    protected void shutDown() {
        script.shutdown();
        overlayManager.remove(overlay);
    }
    
    public void incrementLogsGained() {
        logsGained++;
    }
    
    public int getLogsGained() {
        return logsGained;
    }
    
    public long getRuntime() {
        return System.currentTimeMillis() - startTime;
    }
    
    public int getLogsPerHour() {
        long runtime = getRuntime();
        if (runtime > 0) {
            return (int) ((logsGained * 3600000.0) / runtime);
        }
        return 0;
    }
    
    public int getWoodcuttingXpGained() {
        if (Microbot.getClient().getSkillExperience(net.runelite.api.Skill.WOODCUTTING) > 0) {
            return Microbot.getClient().getSkillExperience(net.runelite.api.Skill.WOODCUTTING) - initialWoodcuttingXp;
        }
        return 0;
    }
    
    public int getWoodcuttingXpPerHour() {
        long runtime = getRuntime();
        if (runtime > 0) {
            return (int) ((getWoodcuttingXpGained() * 3600000.0) / runtime);
        }
        return 0;
    }
    
    public void resetStatistics() {
        startTime = System.currentTimeMillis();
        logsGained = 0;
        if (Microbot.getClient().getSkillExperience(net.runelite.api.Skill.WOODCUTTING) > 0) {
            initialWoodcuttingXp = Microbot.getClient().getSkillExperience(net.runelite.api.Skill.WOODCUTTING);
        }
    }
}