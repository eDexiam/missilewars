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

package de.butzlabben.missilewars.wrapper.abstracts;

import com.google.gson.annotations.SerializedName;
import de.butzlabben.missilewars.Logger;
import de.butzlabben.missilewars.game.Arenas;
import de.butzlabben.missilewars.wrapper.geometry.Area;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

@RequiredArgsConstructor
@Getter
@ToString
@AllArgsConstructor
public class Lobby {

    private String name = "lobby0";
    @SerializedName("display_name") private String displayName = "&eDefault game";
    @SerializedName("world") private String world = Bukkit.getWorlds().get(0).getName();
    @SerializedName("lobby_time") private int lobbyTime = 60;
    @SerializedName("join_ongoing_game") private boolean joinOngoingGame = false;
    @SerializedName("min_size") private int minSize = 2;
    @SerializedName("max_size") private int maxSize = 20;
    @SerializedName("team1_name") private String team1Name = "Team1";
    @SerializedName("team1_color") private String team1Color = "&c";
    @SerializedName("team2_name") private String team2Name = "Team2";
    @SerializedName("team2_color") private String team2Color = "&a";
    @SerializedName("spawn_point") private Location spawnPoint = Bukkit.getWorlds().get(0).getSpawnLocation();
    @SerializedName("after_game_spawn") private Location afterGameSpawn = Bukkit.getWorlds().get(0).getSpawnLocation();
    private Area area = Area.defaultAreaAround(Bukkit.getWorlds().get(0).getSpawnLocation());
    @SerializedName("map_choose_procedure") private MapChooseProcedure mapChooseProcedure = MapChooseProcedure.FIRST;
    @SerializedName("possible_arenas") private List<String> possibleArenas = new ArrayList<String>() {{
        add("arena");
    }};

    @Setter private transient File file;

    public World getBukkitWorld() {
        World world = Bukkit.getWorld(getWorld());
        if (world == null) {
            Logger.ERROR.log("Could not find any world with the name: " + this.world);
            Logger.ERROR.log("Please correct this in the configuration of lobby \"" + name + "\"");
        }
        return world;
    }

    public void checkForWrongArenas() {
        for (String arenaName : possibleArenas) {
            Optional<Arena> arena = Arenas.getFromName(arenaName);
            if (!arena.isPresent()) {
                Logger.WARN.log("Could not find arena with name \"" + arenaName + "\" for lobby \"" + getName() + "\"");
            }
        }
    }

    public List<Arena> getArenas() {
        return possibleArenas
                .stream()
                .map(Arenas::getFromName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}
