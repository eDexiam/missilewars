/*
 * This file is part of MissileWars (https://github.com/Butzlabben/missilewars).
 * Copyright (c) 2018-2021 Daniel Nägele.
 *
 * MissileWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MissileWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MissileWars.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.butzlabben.missilewars.listener;

import de.butzlabben.missilewars.Config;
import de.butzlabben.missilewars.Logger;
import de.butzlabben.missilewars.MessageConfig;
import de.butzlabben.missilewars.MissileWars;
import de.butzlabben.missilewars.game.Game;
import de.butzlabben.missilewars.game.GameManager;
import de.butzlabben.missilewars.game.GameState;
import de.butzlabben.missilewars.util.MotdManager;
import de.butzlabben.missilewars.util.PlayerDataProvider;
import de.butzlabben.missilewars.wrapper.event.PlayerArenaJoinEvent;
import de.butzlabben.missilewars.wrapper.event.PlayerArenaLeaveEvent;
import de.butzlabben.missilewars.wrapper.event.PrePlayerArenaJoinEvent;
import de.butzlabben.missilewars.wrapper.player.MWPlayer;
import de.butzlabben.missilewars.wrapper.signs.MWSign;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

/**
 * @author Butzlabben
 * @since 01.01.2018
 */
@SuppressWarnings("deprecation")
public class PlayerListener implements Listener {

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        if (Config.motdEnabled()) {
            event.setMotd(MotdManager.getInstance().getMotd());
        }
    }

    @EventHandler
    public void on(FoodLevelChangeEvent e) {
        Game game = getGame(e.getEntity().getLocation());
        if (game != null)
            e.setCancelled(true);
    }

    @EventHandler
    public void on(BlockPlaceEvent e) {
        Game game = getGame(e.getPlayer().getLocation());
        if (game != null && e.getPlayer().getGameMode() != GameMode.CREATIVE)
            e.setBuild(false);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        Game game = getGame(e.getPlayer().getLocation());
        if (game != null)
            e.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerPickupItemEvent e) {
        Game game = getGame(e.getPlayer().getLocation());
        if (game != null)
            e.setCancelled(true);
    }

    @EventHandler
    public void on(PlayerQuitEvent e) {
        Player p = e.getPlayer();

        Game game = getGame(p.getLocation());
        if (game != null) {
            if (!game.isIn(game.getLobby().getAfterGameSpawn()))
                p.teleport(game.getLobby().getAfterGameSpawn());
            else
                p.teleport(Config.getFallbackSpawn());
        }
    }

    @EventHandler
    public void on(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        Game game = getGame(p.getLocation());
        if (checkJoinOrLeave(p, null, game)) {
            p.teleport(Config.getFallbackSpawn());
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Game to = getGame(event.getTo());
        Game from = getGame(event.getFrom());
        if (checkJoinOrLeave(event.getPlayer(), from, to)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Game to = getGame(event.getTo());
        Game from = getGame(event.getFrom());
        if (checkJoinOrLeave(event.getPlayer(), from, to)) {
            event.setCancelled(true);
            Vector addTo = event.getFrom().toVector().subtract(event.getTo().toVector()).multiply(3);
            addTo.setY(0);
            event.getPlayer().teleport(event.getFrom().add(addTo));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLeaveWithStuff(PlayerArenaLeaveEvent event) {
        Game game = event.getGame();

        MWPlayer mwPlayer = game.getPlayer(event.getPlayer());
        if (mwPlayer == null)
            return;

        if (mwPlayer.getTeam() != null)
            mwPlayer.getTeam().removeMember(mwPlayer);

        game.getPlayers().remove(event.getPlayer().getUniqueId());

        Player player = mwPlayer.getPlayer();
        if (player == null) {
            Logger.WARN.log("Player was null as he left the game");
            return;
        }

        Scoreboard sb = game.getScoreboard();
        Team scoreboardTeam = sb.getPlayerTeam(player);
        if (scoreboardTeam != null)
            scoreboardTeam.removePlayer(player);

        player.getInventory().clear();
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());

        MissileWars.getInstance().getSignRepository().getSigns(game).forEach(MWSign::update);

        PlayerDataProvider.getInstance().loadInventory(player);
    }

    @EventHandler
    public void onPlayerArenaJoin(PlayerArenaJoinEvent event) {
        Logger.DEBUG.log("PlayerArenaJoinEvent: " + event.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerArenaLeave(PlayerArenaLeaveEvent event) {
        Logger.DEBUG.log("PlayerArenaLeaveEvent: " + event.getPlayer().getName());
    }

    private Game getGame(Location location) {
        if (GameManager.getInstance() == null) return null;

        return GameManager.getInstance().getGame(location);
    }

    /**
     * Checks if cancelled and spits out events
     *
     * @param player
     * @param from
     * @param to
     *
     * @return
     */
    private boolean checkJoinOrLeave(Player player, Game from, Game to) {
        if (to != null && to != from) {
            PrePlayerArenaJoinEvent event = new PrePlayerArenaJoinEvent(player, to);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) player.sendMessage(MessageConfig.getMessage("not_enter_arena"));
            return event.isCancelled();
        }
        if (from != null && to != from) {
            PlayerArenaLeaveEvent event = new PlayerArenaLeaveEvent(player, from);
            Bukkit.getPluginManager().callEvent(event);
        }
        return false;
    }


    // Internal stuff
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMaxPlayers(PrePlayerArenaJoinEvent event) {
        if (event.getGame().getPlayers().size() >= event.getGame().getLobby().getMaxSize() && event.getGame().getState() == GameState.LOBBY)
            event.setCancelled(true);
    }

    // Internal stuff
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(PrePlayerArenaJoinEvent event) {
        Logger.DEBUG.log("PrePlayerArenaJoinEvent: " + event.getPlayer().getName());
        Bukkit.getScheduler().runTask(MissileWars.getInstance(), () -> Bukkit.getPluginManager().callEvent(new PlayerArenaJoinEvent(event.getPlayer(), event.getGame())));
    }
}
