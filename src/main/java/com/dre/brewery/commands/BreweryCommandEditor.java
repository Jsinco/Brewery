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

package com.dre.brewery.commands;

import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import dev.rollczi.litecommands.command.builder.CommandBuilder;
import dev.rollczi.litecommands.command.executor.CommandExecuteService;
import dev.rollczi.litecommands.editor.Editor;
import dev.rollczi.litecommands.platform.Platform;
import dev.rollczi.litecommands.suggestion.SuggestionService;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.ApiStatus;

/**
 * LiteCommands Editor that puts commands under "/breweryx" route, as well as aliases specified in the config
 *
 * @see com.dre.brewery.commands.annotation.BreweryCommand
 * @see CommandManager#CommandManager()
 */
class BreweryCommandEditor implements Editor<CommandSender> {
	@Override
	public CommandBuilder<CommandSender> edit(CommandBuilder<CommandSender> context) {
		Config config = ConfigManager.getConfig(Config.class);

		return context
			.routeName("breweryx " + context.name())
			.routeAliases(
				config.getCommandAliases().stream()
				.map(alias -> alias + " " + context.name())
				.toList()
			);
	}
}
