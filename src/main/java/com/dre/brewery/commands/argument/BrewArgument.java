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

package com.dre.brewery.commands.argument;

import com.dre.brewery.Brew;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.recipe.BRecipe;
import com.dre.brewery.utility.Suppliers;
import dev.rollczi.litecommands.argument.Argument;
import dev.rollczi.litecommands.argument.parser.ParseResult;
import dev.rollczi.litecommands.argument.resolver.ArgumentResolver;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.suggestion.SuggestionContext;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.function.Supplier;

// TODO: Use names
// but imo, ids are better (and easier to implement)
public class BrewArgument extends ArgumentResolver<CommandSender, Brew> {
	private static final Lang lang = ConfigManager.getConfig(Lang.class);

	// lazy initialize, because recipes are loaded after commands
	private final Supplier<List<String>> recipeIds = Suppliers.lazily(
		() -> BRecipe.getAllRecipes().stream().map(BRecipe::getId).toList()
	);

	@Override
	protected ParseResult<Brew> parse(Invocation<CommandSender> invocation, Argument<Brew> argument, String input) {
		BRecipe recipe = BRecipe.getById(input);

		if (recipe == null) {
			return ParseResult.failure(lang.getEntry("Error_NoBrewName", input));
		}

		return ParseResult.success(recipe.createBrew(10)); // we handle quality later
	}

	@Override
	public SuggestionResult suggest(Invocation<CommandSender> invocation, Argument<Brew> argument, SuggestionContext context) {
		return SuggestionResult.of(recipeIds.get());
	}
}
