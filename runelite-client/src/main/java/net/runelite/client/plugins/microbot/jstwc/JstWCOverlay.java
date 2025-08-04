package net.runelite.client.plugins.microbot.jstwc;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class JstWCOverlay extends OverlayPanel {
    
    private final JstWCPlugin plugin;
    private final JstWCScript script;
    
    @Inject
    public JstWCOverlay(JstWCPlugin plugin, JstWCScript script) {
        super(plugin);
        this.plugin = plugin;
        this.script = script;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }
    
    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(250, 200));
            panelComponent.getChildren().clear();
            
            // Title
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("JstWC v" + JstWCScript.version)
                    .color(Color.GREEN)
                    .build());
            
            // Current state
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("State:")
                    .right(script.getCurrentState() != null ? script.getCurrentState().getDescription() : "Unknown")
                    .build());
            
            // Runtime
            long runtime = plugin.getRuntime();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Runtime:")
                    .right(formatTime(runtime))
                    .build());
            
            // Current HP
            int currentHp = Rs2Player.getBoostedSkillLevel(net.runelite.api.Skill.HITPOINTS);
            int maxHp = Rs2Player.getRealSkillLevel(net.runelite.api.Skill.HITPOINTS);
            Color hpColor = currentHp < (maxHp * 0.3) ? Color.RED : 
                           currentHp < (maxHp * 0.6) ? Color.ORANGE : Color.GREEN;
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Health:")
                    .right(currentHp + "/" + maxHp)
                    .rightColor(hpColor)
                    .build());
            
            // Woodcutting level and XP
            int wc = Rs2Player.getRealSkillLevel(net.runelite.api.Skill.WOODCUTTING);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Woodcutting:")
                    .right(String.valueOf(wc))
                    .build());
            
            // XP gained and per hour
            int xpGained = plugin.getWoodcuttingXpGained();
            int xpPerHour = plugin.getWoodcuttingXpPerHour();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("XP gained:")
                    .right(formatNumber(xpGained) + " (" + formatNumber(xpPerHour) + "/hr)")
                    .build());
            
            // Logs gained and per hour
            int logsGained = plugin.getLogsGained();
            int logsPerHour = plugin.getLogsPerHour();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Logs gained:")
                    .right(formatNumber(logsGained) + " (" + formatNumber(logsPerHour) + "/hr)")
                    .build());
            
            // Aggression status
            boolean hasAggro = script.hasAggression();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Aggression:")
                    .right(hasAggro ? "Active" : "Lost")
                    .rightColor(hasAggro ? Color.GREEN : Color.RED)
                    .build());
            
            // Birds attacking count
            int attackingBirds = script.getAttackingBirdsCount();
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Attacking birds:")
                    .right(String.valueOf(attackingBirds))
                    .rightColor(attackingBirds >= 2 ? Color.GREEN : Color.ORANGE)
                    .build());
            
            // Next action
            String nextAction = script.getNextAction();
            if (nextAction != null && !nextAction.isEmpty()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Next action:")
                        .right(nextAction)
                        .build());
            }
            
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        
        return super.render(graphics);
    }
    
    private String formatTime(long milliseconds) {
        long totalSeconds = milliseconds / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
    
    private String formatNumber(int number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        } else {
            return String.valueOf(number);
        }
    }
}