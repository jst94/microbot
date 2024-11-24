package net.runelite.client.plugins.microbot.JstScurrius.Variables;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.JstScurrius.ScurriusConfig;
import net.runelite.client.plugins.microbot.JstScurrius.ScurriusPlugin;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.walker.MovementInteraction;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GraphicsObjectCreated;

public class ScurriusRockfalls {
    @Inject
    Client client;
    @Inject
    ScurriusConfig config;
    @Inject
    ScurriusPlugin plugin;
    HashMap<WorldPoint, GraphicsObject> rockfallLocations = new HashMap();
    private List<WorldPoint> blacklistedTiles = new ArrayList<WorldPoint>();
    private List<WorldPoint> combatTiles = new ArrayList<WorldPoint>();

    public List<WorldPoint> combatTiles() {
        return this.combatTiles;
    }

    public List<WorldPoint> blackListedTiles() {
        return this.blacklistedTiles;
    }

    @Inject
    public ScurriusRockfalls() {
    }

    public void onGameTick() {
        this.rockfallLocations.entrySet().removeIf(entry -> ((GraphicsObject)entry.getValue()).finished());
    }

    public void onGraphicsObjectCreated(GraphicsObjectCreated event) {
        GraphicsObject graphicsObject = event.getGraphicsObject();
        if (graphicsObject.getId() == 2644) {
            WorldPoint rockfall = WorldPoint.fromLocal((Client)this.client, (LocalPoint)graphicsObject.getLocation());
            this.rockfallLocations.put(rockfall, graphicsObject);
        }
    }

    public void getBlacklistedTiles() {
        Optional<NPC> scurrius = Rs2Npc.search().withName("Scurrius").alive().walkable().nearestToPlayer();
        if (scurrius.isEmpty()) {
            this.blacklistedTiles.clear();
            return;
        }
        WorldPoint swTile = scurrius.get().getWorldLocation();
        ArrayList<WorldPoint> tilesUnderScurrius = new ArrayList<WorldPoint>();
        for (int dx = 0; dx < 3; ++dx) {
            for (int dy = 0; dy < 3; ++dy) {
                tilesUnderScurrius.add(new WorldPoint(swTile.getX() + dx, swTile.getY() + dy, swTile.getPlane()));
            }
        }
        WorldPoint actualSWTile = new WorldPoint(swTile.getX() - 1, swTile.getY() - 1, swTile.getPlane());
        WorldPoint actualSETile = new WorldPoint(swTile.getX() + 3, swTile.getY() - 1, swTile.getPlane());
        WorldPoint actualNETile = new WorldPoint(swTile.getX() + 3, swTile.getY() + 3, swTile.getPlane());
        WorldPoint actualNWTile = new WorldPoint(swTile.getX() - 1, swTile.getY() + 3, swTile.getPlane());
        tilesUnderScurrius.add(actualSWTile);
        tilesUnderScurrius.add(actualSETile);
        tilesUnderScurrius.add(actualNETile);
        tilesUnderScurrius.add(actualNWTile);
        this.blacklistedTiles.clear();
        this.blacklistedTiles.addAll(tilesUnderScurrius);
    }

    public void getCombatTiles() {
        Optional<NPC> scurrius = Rs2Npc.search().withName("Scurrius").alive().walkable().nearestToPlayer();
        if (scurrius.isEmpty()) {
            this.combatTiles.clear();
            return;
        }
        WorldPoint south = scurrius.get().getWorldLocation();
        WorldPoint south1 = new WorldPoint(south.getX() - 0, south.getY() - 1, south.getPlane());
        WorldPoint south2 = new WorldPoint(south.getX() + 1, south.getY() - 1, south.getPlane());
        WorldPoint south3 = new WorldPoint(south.getX() + 2, south.getY() - 1, south.getPlane());
        WorldPoint east = scurrius.get().getWorldLocation();
        WorldPoint east1 = new WorldPoint(east.getX() + 3, east.getY() - 0, east.getPlane());
        WorldPoint east2 = new WorldPoint(east.getX() + 3, east.getY() + 1, east.getPlane());
        WorldPoint east3 = new WorldPoint(east.getX() + 3, east.getY() + 2, east.getPlane());
        WorldPoint north = scurrius.get().getWorldLocation();
        WorldPoint north1 = new WorldPoint(north.getX() + 0, north.getY() + 3, north.getPlane());
        WorldPoint north2 = new WorldPoint(north.getX() + 1, north.getY() + 3, north.getPlane());
        WorldPoint north3 = new WorldPoint(north.getX() + 2, north.getY() + 3, north.getPlane());
        WorldPoint west = scurrius.get().getWorldLocation();
        WorldPoint west1 = new WorldPoint(west.getX() - 1, west.getY() + 0, west.getPlane());
        WorldPoint west2 = new WorldPoint(west.getX() - 1, west.getY() + 1, west.getPlane());
        WorldPoint west3 = new WorldPoint(west.getX() - 1, west.getY() + 2, west.getPlane());
        this.combatTiles.clear();
        List<WorldPoint> allCombatTiles = Arrays.asList(east1, east2, east3, south1, south2, south3, north1, north2, north3, west1, west2, west3);
        List<WorldPoint> reachableTiles = Microbot.reachableTiles();
        for (WorldPoint tile : allCombatTiles) {
            if (!reachableTiles.contains(tile)) continue;
            this.combatTiles.add(tile);
        }
    }

    public Set<WorldPoint> getSafeCombatTiles() {
        HashSet<WorldPoint> safeTiles = new HashSet<WorldPoint>();
        HashSet<WorldPoint> dangerousTiles = new HashSet<WorldPoint>(this.getRockfallLocations().keySet());
        for (WorldPoint combatTile : this.combatTiles) {
            if (this.blacklistedTiles.contains(combatTile) || dangerousTiles.contains(combatTile)) continue;
            safeTiles.add(combatTile);
        }
        return safeTiles;
    }

    public Set<WorldPoint> getAllSafeTiles() {
        HashSet<WorldPoint> safeTiles = new HashSet<WorldPoint>();
        HashSet<WorldPoint> dangerousTiles = new HashSet<WorldPoint>(this.getRockfallLocations().keySet());
        List<WorldPoint> allTiles = Microbot.reachableTiles();
        for (WorldPoint tile : allTiles) {
            if (this.blacklistedTiles.contains(tile) || dangerousTiles.contains(tile)) continue;
            safeTiles.add(tile);
        }
        return safeTiles;
    }

    public void moveToSafeTile() {
        Set<WorldPoint> safeTiles = this.plugin.isFarCasting() || this.plugin.areRatsPresent() ? this.getAllSafeTiles() : this.getSafeCombatTiles();
        if (!safeTiles.isEmpty() && !this.plugin.isOnSafeTile()) {
            WorldPoint playerLocation = this.client.getLocalPlayer().getWorldLocation();
            WorldPoint closestSafeTile = null;
            int minDistance = Integer.MAX_VALUE;
            for (WorldPoint safeTile : safeTiles) {
                int distance = safeTile.distanceTo(playerLocation);
                if (distance >= minDistance) continue;
                minDistance = distance;
                closestSafeTile = safeTile;
            }
            if (closestSafeTile != null) {
                MovementInteraction.walkTo(closestSafeTile);
            }
        }
    }

    public HashMap<WorldPoint, GraphicsObject> getRockfallLocations() {
        return this.rockfallLocations;
    }
}
