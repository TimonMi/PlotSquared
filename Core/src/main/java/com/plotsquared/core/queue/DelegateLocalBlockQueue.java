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
package com.plotsquared.core.queue;

import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;

public class DelegateLocalBlockQueue extends LocalBlockQueue {

    private final LocalBlockQueue parent;

    public DelegateLocalBlockQueue(LocalBlockQueue parent) {
        super(parent == null ? null : parent.getWorld());
        this.parent = parent;

        if (parent != null) {
            this.setForceSync(parent.isForceSync());
            this.setChunkObject(parent.getChunkObject());
        }
    }

    public LocalBlockQueue getParent() {
        return parent;
    }

    @Override public boolean next() {
        return parent.next();
    }

    @Override public void startSet(boolean parallel) {
        if (parent != null) {
            parent.startSet(parallel);
        }
    }

    @Override public void endSet(boolean parallel) {
        if (parent != null) {
            parent.endSet(parallel);
        }
    }

    @Override public int size() {
        if (parent != null) {
            return parent.size();
        }
        return 0;
    }

    @Override public void optimize() {
        if (parent != null) {
            parent.optimize();
        }
    }

    @Override public long getModified() {
        if (parent != null) {
            return parent.getModified();
        }
        return 0;
    }

    @Override public void setModified(long modified) {
        if (parent != null) {
            parent.setModified(modified);
        }
    }

    @Override public boolean setBlock(int x, int y, int z, Pattern pattern) {
        return parent.setBlock(x, y, z, pattern);
    }

    @Override public boolean setBlock(int x, int y, int z, BaseBlock id) {
        return parent.setBlock(x, y, z, id);
    }

    @Override public boolean setBlock(int x, int y, int z, BlockState id) {
        return parent.setBlock(x, y, z, id);
    }

    @Override public BlockState getBlock(int x, int y, int z) {
        return parent.getBlock(x, y, z);
    }

    @Override public boolean setBiome(int x, int z, BiomeType biome) {
        return parent.setBiome(x, z, biome);
    }

    @Override public boolean setBiome() {
        return parent.setBiome();
    }

    @Override public String getWorld() {
        return parent.getWorld();
    }

    @Override public void flush() {
        if (parent != null) {
            parent.flush();
        }
    }

    @Override public void refreshChunk(int x, int z) {
        if (parent != null) {
            parent.refreshChunk(x, z);
        }
    }

    @Override public void fixChunkLighting(int x, int z) {
        if (parent != null) {
            parent.fixChunkLighting(x, z);
        }
    }

    @Override public void regenChunk(int x, int z) {
        if (parent != null) {
            parent.regenChunk(x, z);
        }
    }

    @Override public boolean enqueue() {
        if (parent != null) {
            return parent.enqueue();
        }
        return false;
    }
}
