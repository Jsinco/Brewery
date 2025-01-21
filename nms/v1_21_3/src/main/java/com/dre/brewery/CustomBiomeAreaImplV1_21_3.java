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

package com.dre.brewery;

import com.dre.brewery.packets.CustomBiomeArea;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;

public class CustomBiomeAreaImplV1_21_3 extends CustomBiomeArea {

    Server server = Bukkit.getServer();
    CraftServer craftserver = (CraftServer) server;
    //DedicatedServer dedicatedserver = craftserver.getServer();
    //ResourceKey<Biome.BiomeBuilder> newKey = ResourceKey.a(IRegistry.aO, new MinecraftKey("test", "fancybiome"));

    private Biome biome;

    public CustomBiomeAreaImplV1_21_3(Location parentBiomeLocation, String fogColor, String skyColor, String waterColor, String waterFogColor, String foliageColor, String grassColor) {
        Block block=;
        block.setBiome();

        super(parentBiomeLocation, fogColor, skyColor, waterColor, waterFogColor, foliageColor, grassColor);
        CraftWorld craftWorld = (CraftWorld) parentBiomeLocation.getWorld();
        Biome parentBiome = craftWorld.getHandle().getBiome(new BlockPos(parentBiomeLocation.getBlockX(), parentBiomeLocation.getBlockY(), parentBiomeLocation.getBlockZ())).value();
        BiomeSpecialEffects parentSpecialEffects = parentBiome.getSpecialEffects();



        BiomeSpecialEffects specialEffects = new BiomeSpecialEffects.Builder()
            .ambientAdditionsSound(parentSpecialEffects.getAmbientAdditionsSettings().get())
            .ambientMoodSound(parentSpecialEffects.getAmbientMoodSettings().get())
            .ambientLoopSound(parentSpecialEffects.getAmbientLoopSoundEvent().get())
            .fogColor(hexToColor(fogColor))
            .skyColor(hexToColor(skyColor))
            .waterColor(hexToColor(waterColor))
            .waterFogColor(hexToColor(waterFogColor))
            .foliageColorOverride(hexToColor(foliageColor))
            .grassColorOverride(hexToColor(grassColor))
            .build();

        this.biome = new Biome.BiomeBuilder()
            .downfall(0.5F)
            .temperature(parentBiome.getBaseTemperature())
            .specialEffects(specialEffects)
            .generationSettings(parentBiome.getGenerationSettings())
            .hasPrecipitation(parentBiome.hasPrecipitation())
            .mobSpawnSettings(parentBiome.getMobSettings())
            .build();

    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public boolean isRegistered() {
        return false;
    }

}
