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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@BreweryCommand
@Command(name = "copy")
@Permission("brewery.cmd.copy")
public class CopyCommand extends CommandBase {

    public CopyCommand(CommandManager commandManager) {
        super(commandManager);
    }

    @Execute
    public void execute(@Context Player player, @Arg int count) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!Brew.isBrew(hand)) {
            this.lang.sendEntry(player, "Error_ItemNotPotion");
        }
        while (count > 0) {
            ItemStack item = hand.clone();
            if (!(player.getInventory().addItem(item)).isEmpty()) {
                this.lang.sendEntry(player, "CMD_Copy_Error", String.valueOf(count));
                return;
            }
            count--;
        }
    }

}
