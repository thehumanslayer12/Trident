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
package net.tridentsdk.nbt;

import net.tridentsdk.api.nbt.*;
import net.tridentsdk.api.nbt.TagType;

/**
 * @author The TridentSDK Team
 */
public class FloatTag extends net.tridentsdk.api.nbt.NBTTag {
    float value;

    public FloatTag(String name) {
        super(name);
    }

    public float getValue() {
        return this.value;
    }

    public FloatTag setValue(float value) {
        this.value = value;
        return this;
    }

    /* (non-Javadoc)
     * @see net.tridentsdk.api.nbt.NBTTag#getType()
     */
    @Override
    public net.tridentsdk.api.nbt.TagType getType() {
        return TagType.FLOAT;
    }
}
