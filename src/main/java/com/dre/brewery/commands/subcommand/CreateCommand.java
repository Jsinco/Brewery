/*
 * BreweryX Bukkit-Plugin for an alternate brewing process
 * Copyright (C) 2024-2025 The Brewery Team
 *
 * This file is part of BreweryX.
 *
 * BreweryX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BreweryX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BreweryX. If not, see <http://www.gnu.org/licenses/gpl-3.0.html>.
 */

package com.dre.brewery.commands.subcommand;

import com.dre.brewery.Brew;
import com.dre.brewery.commands.CommandBase;
import com.dre.brewery.commands.CommandManager;
import com.dre.brewery.commands.annotation.BreweryCommand;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.permission.Permission;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@BreweryCommand
@Command(name = "create")
@Permission("brewery.cmd.create")
public class CreateCommand extends CommandBase {

    public CreateCommand(CommandManager commandManager) {
        super(commandManager);
    }

    @Execute
    public void execute(
        @Context Player player,
        @Arg Brew brew, @Arg Optional<Integer> quality,
        @Arg Optional<Player> targetPlayer
    ) {
        brew.setQuality(quality.orElse(10));

        giveBrewToPlayer(player, brew, targetPlayer.orElse(player));
    }

    // non-player version of the above
    // notice the lack of Optional<>
    @Execute
    public void execute(
        @Context CommandSender sender,
        @Arg Brew brew,
        @Arg Integer quality,
        @Arg Player player
    ) {
        brew.setQuality(quality);

        giveBrewToPlayer(sender, brew, player);
    }

    private void giveBrewToPlayer(CommandSender sender, Brew brew, Player player) {
        if (player.getInventory().firstEmpty() == -1) {
            this.lang.sendEntry(sender, "CMD_Copy_Error", "1");
        }

        ItemStack item = brew.createItem(null, player);
        if (item == null) {
            return; // original implementation also did nothing, but in theory this shouldn't happen...
        }

        player.getInventory().addItem(item);
        this.lang.sendEntry(sender, "CMD_Created");
    }

}
