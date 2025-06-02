package net.runelite.client.plugins.microbot.JstWildyAgil; // lowercase package

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.plugins.microbot.Microbot; // Nodig voor Microbot.status
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
// Import JstWildyAgilScript en JstWildyAgilConfig als ze nodig zijn voor de overlay
// import net.runelite.client.plugins.microbot.jstwildyagil.JstWildyAgilScript;
// import net.runelite.client.plugins.microbot.jstwildyagil.JstWildyAgilConfig;
// import net.runelite.client.plugins.microbot.jstwildyagil.enums.WildyAgilState; // Als je dit direct gebruikt


public class JstWildyAgilityOverlay extends OverlayPanel { // Moet Overlay of een subklasse daarvan extenden

    private final JstWildyAgilPlugin plugin; // Inject de plugin als je toegang nodig hebt tot de script instance
    private final JstWildyAgilConfig config; // Inject de config als de overlay hierop moet reageren

    @Inject
    JstWildyAgilityOverlay(JstWildyAgilPlugin plugin, JstWildyAgilConfig config) {
        super(plugin); // Roep de super constructor aan met de plugin instance
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT); // Of een andere gewenste positie
        // setNaughty(); // Optioneel: als je niet wilt dat het in screenshots komt
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        // Access the script through the plugin instance
        JstWildyAgilScript script = plugin.getScript();
        
        try {
            panelComponent.setPreferredSize(new Dimension(220, 300));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("JST Wildy Agility V" + (script != null ? JstWildyAgilScript.version : "N/A"))
                    .color(net.runelite.client.ui.ColorScheme.PROGRESS_COMPLETE_COLOR)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Current State:")
                    .right(script != null ? JstWildyAgilScript.SCRIPT_STATE.toString() : "Not Started")
                    .build());
            
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Status:")
                    .right(Microbot.status) 
                    .build());
            
            // Show if token handling is enabled
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Handle Tokens:")
                    .right(config.handleTokens() ? "Yes" : "No")
                    .build());
            
            // Voorbeeld voor tickets, zorg dat AGILITY_ARENA_TICKET_NAME bestaat
            // String ticketName = JstWildyAgilScript.AGILITY_ARENA_TICKET_NAME; // Maak AGILITY_ARENA_TICKET_NAME static of haal het uit config
            // panelComponent.getChildren().add(LineComponent.builder()
            //        .left("Tickets in inv:")
            //        .right(String.valueOf(net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.count(ticketName)))
            //        .build());

        } catch (Exception ex) {
            // System.out.println("Overlay Error: " + ex.getMessage()); // Voor debuggen
            panelComponent.getChildren().add(LineComponent.builder().left("Overlay Error...").build());
        }
        return super.render(graphics);
    }
}