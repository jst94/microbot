package net.runelite.client.plugins.microbot.AutoBankSkillerPlugin;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.plugins.microbot.Microbot;

import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Color;

public class AutoBankSkillerOverlay extends OverlayPanel {
    private final AutoBankSkillerConfig config;
    private final AutoBankSkillerScript script;

    @Inject
    public AutoBankSkillerOverlay(AutoBankSkillerConfig config, AutoBankSkillerScript script) {
        super();
        setPosition(OverlayPosition.TOP_LEFT);
        this.config = config;
        this.script = script;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Auto Bank Skiller")
                .color(Color.WHITE)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Skill:")
                .right(config.skillType().toString())
                .build());

        if (config.skillType() == AutoBankSkillerConfig.SkillType.FLETCHING) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Type:")
                    .right(config.fletchingType().toString())
                    .build());
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Primary Item:")
                .right(String.valueOf(config.itemId()))
                .build());

        if (config.secondaryItemId() != 0) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Secondary Item:")
                    .right(String.valueOf(config.secondaryItemId()))
                    .build());
        }

        if (Microbot.pauseAllScripts) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status:")
                    .right("PAUSED")
                    .rightColor(Color.RED)
                    .build());
        }

        return super.render(graphics);
    }
}
