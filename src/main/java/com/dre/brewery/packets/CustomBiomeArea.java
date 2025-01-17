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

package com.dre.brewery.packets;

import org.bukkit.Location;

public abstract class CustomBiomeArea {

    protected final Location parentBiomeLocation;
    protected String fogColor;
    protected String skyColor;
    protected String waterColor;
    protected String waterFogColor;
    protected String foliageColor;
    protected String grassColor;


    public CustomBiomeArea(Location parentBiomeLocation, String fogColor, String skyColor, String waterColor, String waterFogColor, String foliageColor, String grassColor) {
        this.parentBiomeLocation = parentBiomeLocation;
        this.fogColor = fogColor;
        this.skyColor = skyColor;
        this.waterColor = waterColor;
        this.waterFogColor = waterFogColor;
        this.foliageColor = foliageColor;
        this.grassColor = grassColor;
    }

    public abstract int getId();
    public abstract String getName();
    public abstract boolean isRegistered();

}
