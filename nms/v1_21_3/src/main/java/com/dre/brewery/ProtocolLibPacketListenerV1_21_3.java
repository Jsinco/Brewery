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

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.dre.brewery.packets.ChunkBiomePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.server.packs.repository.Pack;

public class ProtocolLibPacketListenerV1_21_3 implements ChunkBiomePacketListener<PacketContainer> {

    private final ProtocolManager protocolManager;

    public ProtocolLibPacketListenerV1_21_3() {
        this.protocolManager = ProtocolLibrary.getProtocolManager();

        protocolManager.addPacketListener(new PacketAdapter(BreweryPlugin.getInstance(),
            ListenerPriority.NORMAL,
            PacketType.Play.Server.CHUNKS_BIOMES) {
            @Override
            public void onPacketSending(PacketEvent event) {
                // Something like, if cauldron is preset and xyz location, change the biome in that area to a custom biome
                PacketContainer packet = event.getPacket();
                packet.
            }
        });
    }

}
