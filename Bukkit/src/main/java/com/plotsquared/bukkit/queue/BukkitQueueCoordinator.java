/*
 *       _____  _       _    _____                                _
 *      |  __ \| |     | |  / ____|                              | |
 *      | |__) | | ___ | |_| (___   __ _ _   _  __ _ _ __ ___  __| |
 *      |  ___/| |/ _ \| __|\___ \ / _` | | | |/ _` | '__/ _ \/ _` |
 *      | |    | | (_) | |_ ____) | (_| | |_| | (_| | | |  __/ (_| |
 *      |_|    |_|\___/ \__|_____/ \__, |\__,_|\__,_|_|  \___|\__,_|
 *                                    | |
 *                                    |_|
 *            PlotSquared plot management system for Minecraft
 *                  Copyright (C) 2020 IntellectualSites
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.plotsquared.bukkit.queue;

import com.google.inject.Inject;
import com.plotsquared.bukkit.schematic.StateWrapper;
import com.plotsquared.bukkit.util.BukkitBlockUtil;
import com.plotsquared.core.inject.factory.ChunkCoordinatorBuilderFactory;
import com.plotsquared.core.inject.factory.ChunkCoordinatorFactory;
import com.plotsquared.core.queue.BasicQueueCoordinator;
import com.plotsquared.core.queue.ChunkCoordinator;
import com.plotsquared.core.queue.LocalChunk;
import com.plotsquared.core.util.BlockUtil;
import com.plotsquared.core.util.ChunkUtil;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

public class BukkitQueueCoordinator extends BasicQueueCoordinator {

    private final SideEffectSet sideEffectSet;
    private org.bukkit.World bukkitWorld;
    @Inject private ChunkCoordinatorBuilderFactory chunkCoordinatorBuilderFactory;
    @Inject private ChunkCoordinatorFactory chunkCoordinatorFactory;
    private Runnable whenDone;
    private ChunkCoordinator chunkCoordinator;

    @Inject public BukkitQueueCoordinator(World world) {
        super(world);
        sideEffectSet = SideEffectSet.none().with(SideEffect.LIGHTING, SideEffect.State.OFF)
            .with(SideEffect.NEIGHBORS, SideEffect.State.OFF);
    }

    @Override public BlockState getBlock(int x, int y, int z) {
        org.bukkit.World worldObj = BukkitAdapter.adapt(getWorld());
        if (worldObj != null) {
            Block block = worldObj.getBlockAt(x, y, z);
            return BukkitBlockUtil.get(block);
        } else {
            return BlockUtil.get(0, 0);
        }
    }

    @Override public void start() {
        chunkCoordinator.start();
    }

    //TODO: implement cancellation
    @Override public void cancel() {
        chunkCoordinator.cancel();
    }

    @Override public boolean enqueue() {
        final Clipboard regenClipboard;
        if (isRegen()) {
            Region region = new CuboidRegion(
                BlockVector3.at(getRegenStart()[0] << 4, 0, getRegenStart()[1] << 4),
                BlockVector3.at((getRegenEnd()[0] << 4) + 15, 255, (getRegenEnd()[1] << 4) + 15));
            regenClipboard = new BlockArrayClipboard(region);
            getWorld().regenerate(region, regenClipboard);
        } else {
            regenClipboard = null;
        }
        Consumer<BlockVector2> consumer = getChunkConsumer();
        if (consumer == null) {
            consumer = blockVector2 -> {
                LocalChunk localChunk = getBlockChunks().get(blockVector2);
                boolean isRegenChunk =
                    regenClipboard != null && blockVector2.getBlockX() > getRegenStart()[0]
                        && blockVector2.getBlockZ() > getRegenStart()[1]
                        && blockVector2.getBlockX() < getRegenEnd()[0]
                        && blockVector2.getBlockZ() < getRegenEnd()[1];
                if (isRegenChunk) {
                    for (int layer = 0; layer < 16; layer++) {
                        for (int y = layer << 4; y < 16; y++) {
                            for (int x = 0; x < 16; x++) {
                                for (int z = 0; z < 16; z++) {
                                    BaseBlock block =
                                        regenClipboard.getFullBlock(BlockVector3.at(x, y, z));
                                    setWorldBlock(x, y, z, block, blockVector2);
                                }
                            }
                        }
                    }
                    return;
                } else if (localChunk == null) {
                    throw new NullPointerException(
                        "LocalChunk cannot be null when accessed from ChunkCoordinator");
                }
                int sx = blockVector2.getX() << 4;
                int sz = blockVector2.getZ() << 4;
                for (int layer = 0; layer < localChunk.getBaseblocks().length; layer++) {
                    BaseBlock[] blocksLayer = localChunk.getBaseblocks()[layer];
                    if (blocksLayer == null) {
                        continue;
                    }
                    for (int j = 0; j < blocksLayer.length; j++) {
                        if (blocksLayer[j] == null) {
                            continue;
                        }
                        BaseBlock block = blocksLayer[j];
                        int x = sx + ChunkUtil.getX(j);
                        int y = ChunkUtil.getY(layer, j);
                        int z = sz + ChunkUtil.getZ(j);
                        setWorldBlock(x, y, z, block, blockVector2);
                    }
                }
                for (int layer = 0; layer < localChunk.getBaseblocks().length; layer++) {
                    BiomeType[] biomesLayer = localChunk.getBiomes()[layer];
                    if (biomesLayer == null) {
                        continue;
                    }
                    for (int j = 0; j < biomesLayer.length; j++) {
                        if (biomesLayer[j] == null) {
                            continue;
                        }
                        BiomeType biome = biomesLayer[j];
                        int x = sx + ChunkUtil.getX(j);
                        int y = ChunkUtil.getY(layer, j);
                        int z = sz + ChunkUtil.getZ(j);
                        getWorld().setBiome(BlockVector3.at(x, y, z), biome);
                    }
                }
                if (localChunk.getTiles().size() > 0) {
                    localChunk.getTiles().forEach(((blockVector3, tag) -> {
                        try {
                            BaseBlock block = getWorld().getBlock(blockVector3).toBaseBlock(tag);
                            getWorld().setBlock(blockVector3, block, sideEffectSet);
                        } catch (WorldEditException ignored) {
                            StateWrapper sw = new StateWrapper(tag);
                            sw.restoreTag(getWorld().getName(), blockVector3.getX(),
                                blockVector3.getY(), blockVector3.getZ());
                        }
                    }));
                }
                if (localChunk.getEntities().size() > 0) {
                    localChunk.getEntities().forEach((location, entity) -> {
                        getWorld().createEntity(location, entity);
                    });
                }
            };
        }
        CuboidRegion region;
        Collection<BlockVector2> read = new ArrayList<>();
        if ((region = getReadRegion()) != null) {
            read = region.getChunks();
        }
        chunkCoordinator =
            chunkCoordinatorBuilderFactory.create(chunkCoordinatorFactory).inWorld(getWorld())
                .withChunks(getBlockChunks().keySet()).withChunks(read).withInitialBatchSize(3)
                .withMaxIterationTime(40).withThrowableConsumer(Throwable::printStackTrace)
                .withFinalAction(whenDone).withConsumer(consumer).unloadAfter(isUnloadAfter())
                .build();
        return super.enqueue();
    }

    private void setWorldBlock(int x, int y, int z, BaseBlock block, BlockVector2 blockVector2) {
        try {
            getWorld().setBlock(BlockVector3.at(x, y, z), block, sideEffectSet);
        } catch (WorldEditException ignored) {
            // Fallback to not so nice method
            BlockData blockData = BukkitAdapter.adapt(block);

            if (bukkitWorld == null) {
                bukkitWorld = Bukkit.getWorld(getWorld().getName());
            }
            Chunk chunk = bukkitWorld.getChunkAt(blockVector2.getX(), blockVector2.getZ());

            Block existing = chunk.getBlock(x, y, z);
            final BlockState existingBaseBlock = BukkitAdapter.adapt(existing.getBlockData());
            if (BukkitBlockUtil.get(existing).equals(existingBaseBlock) && existing.getBlockData()
                .matches(blockData)) {
                return;
            }

            if (existing.getState() instanceof Container) {
                ((Container) existing.getState()).getInventory().clear();
            }

            existing.setType(BukkitAdapter.adapt(block.getBlockType()), false);
            existing.setBlockData(blockData, false);
            if (block.hasNbtData()) {
                CompoundTag tag = block.getNbtData();
                StateWrapper sw = new StateWrapper(tag);

                sw.restoreTag(getWorld().getName(), existing.getX(), existing.getY(),
                    existing.getZ());
            }
        }
    }

    @Override public void setCompleteTask(Runnable whenDone) {
        this.whenDone = whenDone;
    }

}