package net.runelite.client.plugins.microbot.JstWildyAgil; // lowercase package

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
// Import JstWildyAgilScript if it's not in the same package, assuming it is for now.
// import net.runelite.client.plugins.microbot.jstwildyagil.JstWildyAgilScript; 


@PluginDescriptor(
        name = PluginDescriptor.Mocrosoft + "WildyAgil", // name = "jstWildyAgil", UI name can be set via properties or kept as is if desired.
        description = "Microbot Wilderness Agility Course runner by JST",
        tags = {"microbot", "agility", "wilderness", "jst"},
        enabledByDefault = false,
        developerPlugin = true // Optioneel: als dit een dev plugin is
)
public class JstWildyAgilPlugin extends Plugin {
    @Inject
    private JstWildyAgilConfig config; // Behoud config injectie
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private JstWildyAgilityOverlay jstWildyAgilOverlay; // Injectie van de overlay

    private JstWildyAgilScript jstWildyAgilScript;

    @Provides
    JstWildyAgilConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(JstWildyAgilConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        if (overlayManager != null && jstWildyAgilOverlay != null) {
            overlayManager.add(jstWildyAgilOverlay);
        }
        jstWildyAgilScript = new JstWildyAgilScript();
        jstWildyAgilScript.run(config); // config wordt hier doorgegeven
    }

    @Override
    protected void shutDown() throws Exception {
        if (jstWildyAgilScript != null) {
            jstWildyAgilScript.shutdown();
        }
        if (overlayManager != null && jstWildyAgilOverlay != null) {
            overlayManager.remove(jstWildyAgilOverlay);
        }
    }

    // Add getter for the overlay to access the script
    public JstWildyAgilScript getScript() {
        return jstWildyAgilScript;
    }
}