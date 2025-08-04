package net.runelite.client.plugins.microbot.jstwinter;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class JstWinterOverlay extends OverlayPanel {
    private final Client client;
    private final JstWinterPlugin plugin;
    private final JstWinterConfig config;

    @Inject
    JstWinterOverlay(Client client, JstWinterPlugin plugin, JstWinterConfig config) {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.setPreferredSize(new Dimension(240, 420));
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("JstWinter v" + JstWinterScript.version)
                .color(Color.GREEN)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("State:")
                .right(JstWinterScript.state.toString())
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Time:")
                .right(plugin.getTimeRunning())
                .build());

        // Game Stats Section
        panelComponent.getChildren().add(LineComponent.builder()
                .left("--- Game Stats ---")
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Won/Lost:")
                .right(plugin.getWon() + "/" + plugin.getLost())
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Total Points:")
                .right(String.valueOf(plugin.getPointsGained()))
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Current Points:")
                .right(JstWinterScript.instance != null ? String.valueOf(JstWinterScript.instance.currentPoints) : "0")
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Warmth/WT HP:")
                .right((JstWinterScript.instance != null ? JstWinterScript.instance.getWarmthLevel() : "0") + "%/" + 
                      (JstWinterScript.instance != null ? JstWinterScript.instance.wintertodtHp : "0") + "%")
                .build());

        // Activity Stats Section
        panelComponent.getChildren().add(LineComponent.builder()
                .left("--- Activities ---")
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Roots/Kindling:")
                .right(plugin.getRootsChopped() + "/" + plugin.getKindlingMade())
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Braziers Lit/Fixed:")
                .right(plugin.getBraziersLit() + "/" + plugin.getBraziersFixed())
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Food/Banking:")
                .right(plugin.getFoodConsumed() + "/" + plugin.getTimesBanked())
                .build());

        // Rates Section
        panelComponent.getChildren().add(LineComponent.builder()
                .left("--- Rates/Hour ---")
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Points/hr:")
                .right(String.format("%,d", plugin.getPointsPerHour()))
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Crates/hr:")
                .right(String.format("%.1f", plugin.getCratesPerHour() / 1.0))
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("FM XP/hr:")
                .right(String.format("%,d", plugin.getFiremakingXpPerHour()))
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Total XP/hr:")
                .right(String.format("%,d", plugin.getTotalXpPerHour()))
                .build());

        // Brazier info
        if (config.smartBrazierSelection() && JstWinterScript.instance != null && JstWinterScript.instance.currentSelectedBrazier != null) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Brazier:")
                    .right(JstWinterScript.instance.currentSelectedBrazier.name().replace("_", " "))
                    .build());
        }

        // Draw brazier location
        if (config.brazierLocation() != null) {
            WorldPoint brazierPoint = config.brazierLocation().getBrazierLocation();
            if (brazierPoint != null && client.getLocalPlayer() != null) {
                LocalPoint localPoint = LocalPoint.fromWorld(client, brazierPoint);
                if (localPoint != null) {
                    Point canvasPoint = Perspective.localToCanvas(client, localPoint, client.getPlane());
                    if (canvasPoint != null) {
                        graphics.setColor(Color.CYAN);
                        graphics.drawString("Brazier", canvasPoint.getX() - 20, canvasPoint.getY() - 10);
                    }
                }
            }
        }

        return super.render(graphics);
    }
}