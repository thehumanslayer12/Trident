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

import com.google.common.collect.Lists;
import net.tridentsdk.api.nbt.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author The TridentSDK Team
 */
public class ListTag extends net.tridentsdk.api.nbt.NBTTag implements TagContainer {
    final List<net.tridentsdk.api.nbt.NBTTag> tags = new ArrayList<>();
    final TagType innerType;

    public ListTag(String name, TagType innerType) {
        super(name);
        this.innerType = innerType;
    }

    public List<net.tridentsdk.api.nbt.NBTTag> listTags() {
        return Lists.newArrayList(this.tags);
    }

    public net.tridentsdk.api.nbt.NBTTag getTag(int index) {
        return this.tags.get(index);
    }

    public void clearTags() {
        this.tags.clear();
    }

    public boolean containsTag(net.tridentsdk.api.nbt.NBTTag tag) {
        return this.tags.contains(tag);
    }

    @Override
    public void addTag(net.tridentsdk.api.nbt.NBTTag tag) {
        if (tag.getType() == this.innerType) {
            this.tags.add(tag);
        }
    }

    public void removeTag(net.tridentsdk.api.nbt.NBTTag tag) {
        this.tags.remove(tag);
    }

    public TagType getInnerType() {
        return this.innerType;
    }

    /* (non-Javadoc)
     * @see net.tridentsdk.api.nbt.NBTTag#getType()
     */
    @Override
    public TagType getType() {
        // TODO Auto-generated method stub
        return TagType.LIST;
    }
}
