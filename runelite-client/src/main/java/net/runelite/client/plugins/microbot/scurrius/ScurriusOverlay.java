package net.runelite.client.plugins.microbot.scurrius;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class ScurriusOverlay extends OverlayPanel {
    private static final TitleComponent TITLE_COMPONENT = TitleComponent.builder()
            .text("Micro Scurrius V" + ScurriusScript.version)
            .color(Color.GREEN)
            .build();
    private static final LineComponent SEPARATOR_COMPONENT = LineComponent.builder().build();

    @Inject
    ScurriusOverlay(ScurriusPlugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setPreferredSize(new Dimension(200, 300));
            panelComponent.getChildren().add(TITLE_COMPONENT);
            panelComponent.getChildren().add(SEPARATOR_COMPONENT);
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(ScurriusScript.state.toString())
                    .build());
        } catch (Exception ex) {
            Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
        }
        return super.render(graphics);
    }
}
