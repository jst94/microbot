package net.runelite.client.plugins.microbot.jstplugins.scurrius;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class ScurriusOverlay extends OverlayPanel {
    private final ScurriusScript script;

    @Inject
    ScurriusOverlay(ScurriusPlugin plugin, ScurriusScript script) {
        super(plugin);
        this.script = script;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(200, 300));
            
            // Title
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Scurrius V" + ScurriusScript.VERSION)
                    .color(Color.GREEN)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder().build());

            // Current state
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("State:")
                    .right(script.state.toString())
                    .leftColor(Color.WHITE)
                    .rightColor(Color.GREEN)
                    .build());

        } catch(Exception ex) {
            System.out.println(ex.getMessage());
        }
        return super.render(graphics);
    }
}
