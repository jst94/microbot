package net.runelite.client.plugins.microbot.JstScurrius.Variables;

import net.runelite.client.plugins.microbot.JstScurrius.ScurriusConfig;
import net.runelite.client.plugins.microbot.JstScurrius.ScurriusPlugin;
import net.runelite.client.plugins.microbot.JstScurrius.Variables.ScurriusRockfalls;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class ScurriusTileOverlays
extends Overlay {
    @Inject
    public ScurriusConfig config;
    private final ScurriusPlugin plugin;
    private final ScurriusRockfalls rockfalls;
    private final Client client;

    @Inject
    public ScurriusTileOverlays(ScurriusPlugin plugin, Client client, ScurriusRockfalls rockfalls, ScurriusConfig config) {
        this.plugin = plugin;
        this.client = client;
        this.rockfalls = rockfalls;
        this.config = config;
        this.setPosition(OverlayPosition.DYNAMIC);
        this.setLayer(OverlayLayer.ABOVE_SCENE);
    }

    public Dimension render(Graphics2D graphics) {
        if (this.config == null) {
            return null;
        }
        if (this.config.enableRockFallDebug() && this.rockfalls != null && this.rockfalls.rockfallLocations != null) {
            for (WorldPoint rockFall : this.rockfalls.rockfallLocations.keySet()) {
                if (rockFall == null) continue;
                this.renderTile(graphics, LocalPoint.fromWorld((Client)this.client, (WorldPoint)rockFall), new Color(255, 0, 0, 52), 2.0, new Color(255, 0, 0, 37), true, 5);
            }
        }
        if (this.plugin.isInInstance() && this.config.enableRockFallDebug()) {
            for (WorldPoint safeTiles : this.rockfalls.getAllSafeTiles()) {
                if (safeTiles == null) continue;
                this.renderTile(graphics, LocalPoint.fromWorld((Client)this.client, (WorldPoint)safeTiles), new Color(0, 255, 0, 52), 2.0, new Color(0, 255, 0, 37), true, 5);
            }
        }
        if (this.plugin.blackListedTiles() != null && this.config.enableRockFallDebug()) {
            for (WorldPoint blacklistedTile : this.plugin.blackListedTiles()) {
                this.renderTile(graphics, LocalPoint.fromWorld((Client)this.client, (WorldPoint)blacklistedTile), new Color(255, 251, 0, 0), 2.0, new Color(255, 251, 0, 0), true, 1);
            }
        }
        return null;
    }

    private void renderTile(Graphics2D graphics, LocalPoint dest, Color color, double borderWidth, Color fillColor, boolean cornersOnly, int divisor) {
        if (dest == null) {
            return;
        }
        Polygon poly = Perspective.getCanvasTilePoly((Client)this.client, (LocalPoint)dest);
        if (poly == null) {
            return;
        }
        if (cornersOnly) {
            ScurriusTileOverlays.renderPolygonCorners(graphics, poly, color, fillColor, new BasicStroke((float)borderWidth), divisor);
        } else {
            OverlayUtil.renderPolygon((Graphics2D)graphics, (Shape)poly, (Color)color, (Color)fillColor, (Stroke)new BasicStroke((float)borderWidth));
        }
    }

    public static void renderPolygonCorners(Graphics2D graphics, Polygon poly, Color color, Color fillColor, Stroke borderStroke, int divisor) {
        graphics.setColor(color);
        Stroke originalStroke = graphics.getStroke();
        graphics.setStroke(borderStroke);
        for (int i = 0; i < poly.npoints; ++i) {
            int ptx = poly.xpoints[i];
            int pty = poly.ypoints[i];
            int prev = i - 1 < 0 ? poly.npoints - 1 : i - 1;
            int next = i + 1 > poly.npoints - 1 ? 0 : i + 1;
            int ptxN = (poly.xpoints[next] - ptx) / divisor + ptx;
            int ptyN = (poly.ypoints[next] - pty) / divisor + pty;
            int ptxP = (poly.xpoints[prev] - ptx) / divisor + ptx;
            int ptyP = (poly.ypoints[prev] - pty) / divisor + pty;
            graphics.drawLine(ptx, pty, ptxN, ptyN);
            graphics.drawLine(ptx, pty, ptxP, ptyP);
        }
        graphics.setColor(fillColor);
        graphics.fill(poly);
        graphics.setStroke(originalStroke);
    }
}
