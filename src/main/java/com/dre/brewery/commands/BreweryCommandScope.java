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

import com.dre.brewery.commands.annotation.BreweryCommand;
import dev.rollczi.litecommands.meta.Meta;
import dev.rollczi.litecommands.scope.Scope;
import dev.rollczi.litecommands.scope.Scopeable;

/**
 * LiteCommands Scope resolving commands annotated with {@link BreweryCommand}
 *
 * @see BreweryCommandEditor
 * @see CommandManager
 */
class BreweryCommandScope implements Scope {
	@Override
	public boolean isApplicable(Scopeable scopeable) {
		return scopeable.meta().get(Meta.COMMAND_ORIGIN_TYPE).stream()
			.anyMatch(clazz -> clazz.isAnnotationPresent(BreweryCommand.class));
	}
}