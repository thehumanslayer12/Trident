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
package net.tridentsdk.event.entity;

import net.tridentsdk.api.entity.Entity;
import net.tridentsdk.api.event.entity.*;

/**
 * Called when an entity damages another entity
 */
public class EntityDamageEntityEvent extends net.tridentsdk.api.event.entity.EntityDamageEvent {
    private final Entity damager;

    public EntityDamageEntityEvent(Entity entity, Entity damager, double damage, Cause cause) {
        super(entity, damage, cause);
        this.damager = damager;
    }

    public Entity getDamager() {
        return this.damager;
    }
}
