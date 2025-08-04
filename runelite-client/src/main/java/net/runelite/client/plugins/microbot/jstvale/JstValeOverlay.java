package net.runelite.client.plugins.microbot.jstvale;

import net.runelite.api.Point;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.jstvale.enums.JstValeState;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class JstValeOverlay extends OverlayPanel {
    
    private final JstValePlugin plugin;
    private final JstValeScript script;

    @Inject
    JstValeOverlay(JstValePlugin plugin, JstValeScript script) {
        super(plugin);
        this.plugin = plugin;
        this.script = script;
        setPosition(OverlayPosition.TOP_LEFT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(200, 300));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Vale Totems v" + script.version)
                    .color(Color.GREEN)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder().build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left(Microbot.status)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("State: " + script.getState())
                    .build());

            panelComponent.getChildren().add(LineComponent.builder().build());

            // Statistics
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Totems completed:")
                    .right(String.valueOf(plugin.getTotemsCompleted()))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Runs completed:")
                    .right(String.valueOf(plugin.getRunsCompleted()))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Offerings collected:")
                    .right(String.valueOf(plugin.getOfferingsCollected()))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder().build());

            // Per hour rates
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Totems/hr:")
                    .right(String.valueOf(plugin.getTotemsPerHour()))
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Runs/hr:")
                    .right(String.valueOf(plugin.getRunsPerHour()))
                    .build());

            // Runtime
            long runtime = System.currentTimeMillis() - plugin.getStartTime();
            String formattedTime = formatTime(runtime);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Runtime:")
                    .right(formattedTime)
                    .build());

            // Current totem
            if (script.getCurrentTotem() != null) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Current totem:")
                        .right(script.getCurrentTotem().getName())
                        .build());
            }

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return super.render(graphics);
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}