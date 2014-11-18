/*
 *     TridentSDK - A Minecraft Server API
 *     Copyright (C) 2014, The TridentSDK Team
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
package net.tridentsdk.nbt.builder;

import net.tridentsdk.api.nbt.CompoundTag;
import net.tridentsdk.api.nbt.builder.CompoundTagBuilder;

/**
 * @author The TridentSDK Team
 */
public class NBTBuilder {
    final CompoundTag base;

    private NBTBuilder(CompoundTag base) {
        this.base = base;
    }

    private NBTBuilder(String name) {
        this(new CompoundTag(name));
    }

    public static CompoundTagBuilder<NBTBuilder> newBase(String name) {
        return new NBTBuilder(name).begin();
    }

    public static CompoundTagBuilder<NBTBuilder> fromBase(CompoundTag tag) {
        return new NBTBuilder(tag).begin();
    }

    private CompoundTagBuilder<NBTBuilder> begin() {
        return new CompoundTagBuilder<>(this.base, this);
    }

    public CompoundTag build() {
        return this.base;
    }
}

