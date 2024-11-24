package net.runelite.client.plugins.microbot.JstScurrius;

import net.runelite.client.plugins.microbot.JstScurrius.Enums.UiLayoutOption;
import net.runelite.client.plugins.microbot.JstScurrius.ScurriusPlugin;
import com.google.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import javax.imageio.ImageIO;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class ScurriusOverlay extends OverlayPanel {
    private final ScurriusPlugin plugin;
    private final Color backgroundColour = new Color(0, 0, 0, 150);
    private BufferedImage logoImage;

    @Inject
    public ScurriusOverlay(ScurriusPlugin plugin) {
        super((Plugin)plugin);
        this.plugin = plugin;
        this.setPosition(OverlayPosition.TOP_LEFT);
        this.setResizable(true);
        this.setMovable(true);
        this.setSnappable(true);
        try {
            InputStream imageStream = getClass().getResourceAsStream("/microbot-scurrius.png");
            if (imageStream != null) {
                this.logoImage = ImageIO.read(imageStream);
            } else {
                System.err.println("Image not found in resources.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.panelComponent.setBackgroundColor(this.backgroundColour);
    }

    public Dimension render(Graphics2D graphics) {
        UiLayoutOption selectedLayout = this.plugin.config.uiLayout();
        this.panelComponent.getChildren().clear();
        switch (selectedLayout) {
            case FULL:
                this.addFullOverlay();
                break;
            case SIMPLE:
                this.addSimpleOverlay();
                break;
            case DEBUG:
                this.addDebugOverlay(graphics);
                break;
        }
        return super.render(graphics);
    }

    private void addFullOverlay() {
        if (this.logoImage != null) {
            ImageComponent logoComponent = new ImageComponent(this.logoImage);
            logoComponent.setPreferredSize(new Dimension(275, 50));
            this.panelComponent.getChildren().add(logoComponent);
        }
        this.panelComponent.setPreferredSize(new Dimension(250, 200));
        this.panelComponent.getChildren().add(TitleComponent.builder()
            .text("Auto Scurrius by Microbot")
            .color(new Color(174, 156, 216))
            .build());
        
        if (this.plugin.isStarted()) {
            this.panelComponent.getChildren().add(TitleComponent.builder()
                .text("Plugin Enabled")
                .color(Color.GREEN)
                .build());
        } else {
            this.panelComponent.getChildren().add(TitleComponent.builder()
                .text("Plugin Disabled")
                .color(Color.RED)
                .build());
        }

        Duration runtime = Duration.between(this.plugin.getStartTime(), Instant.now());
        double hoursElapsed = runtime.getSeconds() / 3600.0;
        int totalKillCount = this.plugin.getKillCount();
        int killsPerHour = (int)(totalKillCount / hoursElapsed);
        if (hoursElapsed > 0.0) {
            killsPerHour = (int)(totalKillCount / hoursElapsed);
        }

        String runtimeStr = this.formatDuration(runtime);
        this.panelComponent.getChildren().add(LineComponent.builder()
            .left("Runtime:")
            .leftColor(Color.WHITE)
            .right(runtimeStr)
            .rightColor(Color.WHITE)
            .build());

        String state = this.plugin.getCurrentState().toString();
        this.panelComponent.getChildren().add(LineComponent.builder()
            .left("State:")
            .right(state)
            .leftColor(Color.WHITE)
            .build());

        String killCount = String.format("%d (%d/hr)", totalKillCount, killsPerHour);
        this.panelComponent.getChildren().add(LineComponent.builder()
            .left("Total Kills (K/PH):")
            .right(killCount)
            .leftColor(Color.WHITE)
            .build());

        this.panelComponent.getChildren().add(LineComponent.builder()
            .left("Tick Delay:")
            .right(String.valueOf(this.plugin.timeout))
            .leftColor(Color.WHITE)
            .build());
    }

    private void addSimpleOverlay() {
        this.panelComponent.setPreferredSize(new Dimension(150, 30));
        String title = "Auto Scurrius by Microbot";
        this.panelComponent.getChildren().add(TitleComponent.builder()
            .text(title)
            .color(Color.cyan)
            .build());
        this.panelComponent.getChildren().add(TitleComponent.builder()
            .text("Enabled")
            .color(Color.GREEN)
            .build());
    }

    private void addDebugOverlay(Graphics2D graphics) {
        this.panelComponent.setPreferredSize(new Dimension(250, 200));
        this.panelComponent.getChildren().add(TitleComponent.builder()
            .text("Scurrius v1.1 Debug ")
            .color(Color.WHITE)
            .build());
        this.addDebugLine("Mage ticks: " + this.plugin.mageTicks);
        this.addDebugLine("Ranged ticks: " + this.plugin.rangeTicks);
        this.addDebugLine("Time Running: " + this.formatDuration(Duration.between(this.plugin.getStartTime(), Instant.now())));
        this.addDebugLine("Has looted items: " + this.plugin.hasLootedItemsInInventory());
    }

    private void addDebugLine(String text) {
        this.panelComponent.getChildren().add(LineComponent.builder()
            .left(text)
            .leftColor(Color.WHITE)
            .build());
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
