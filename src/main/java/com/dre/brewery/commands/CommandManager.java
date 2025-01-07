/*
 * BreweryX Bukkit-Plugin for an alternate brewing process
 * Copyright (C) 2024 The Brewery Team
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

package com.dre.brewery.commands;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.commands.subcommand.CopyCommand;
import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import dev.rollczi.litecommands.jakarta.LiteJakartaExtension;
import lombok.Getter;
import org.bukkit.command.CommandSender;

@Getter
public class CommandManager {
	private final LiteCommands<CommandSender> liteCommands;

	public CommandManager() {
		BreweryPlugin plugin = BreweryPlugin.getInstance();

		this.liteCommands = LiteBukkitFactory.builder("breweryx", plugin)
			.extension(new LiteJakartaExtension<>(), configuration -> configuration
				.violationMessage((invocation, violation) -> "Invalid value for " + violation.getFormattedParameterName())
			)
			.editor(new BreweryCommandScope(), new BreweryCommandEditor())
			.commands(
				new CopyCommand()

			)
			.build();
	}

	public void disable() {
		this.liteCommands.unregister();
	}
}
