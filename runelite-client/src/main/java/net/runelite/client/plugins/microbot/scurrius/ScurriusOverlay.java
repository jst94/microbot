package net.runelite.client.plugins.microbot.scurrius;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class ScurriusOverlay extends OverlayPanel {
    @Inject
    ScurriusOverlay(ScurriusPlugin plugin)
    {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
    }
    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(200, 300));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Micro Scurrius V" + ScurriusScript.VERSION)
                    .color(Color.GREEN)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder().build());

            // Existing state line
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(ScurriusScript.state.toString())
                    .build());

            // New JST status line
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("JST " + ScurriusScript.state.toString())
                    .leftColor(Color.GREEN)
                    .build());

        } catch(Exception ex) {
            System.out.println(ex.getMessage());
        }
        return super.render(graphics);
    }
}
