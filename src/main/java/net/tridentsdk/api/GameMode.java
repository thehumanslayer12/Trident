/*
 *     Trident - A Multithreaded Server Alternative
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
package net.tridentsdk.api;

/**
 * Minecraft game modes
 * <p/>
 * <p>If you need more help, take a look at <a href="http://minecraft.gamepedia.com/Gameplay#Game_modes">Wiki</a></p>
 */
public enum GameMode {
    SURVIVAL(0),
    CREATIVE(1),
    ADVENTURE(2),
    SPECTATE(3),
    HARDCORE(0x8);

    private final byte b;

    GameMode(int i) {
        this.b = (byte) i;
    }

    /**
     * Returns the {@code byte} value of the GameMode
     *
     * @return {@code byte} value of the GameMode
     */
    public byte toByte() {
        return this.b;
    }

    /**
     * Returns the {@code byte} value of the GameMode
     *
     * @param gameMode GameMode
     * @return {@code byte} value of the GameMode
     */
    public static byte toByte(GameMode gameMode) {
        return gameMode.toByte();
    }

    public static GameMode getGameMode(int i) {
        for (GameMode mode : values()) {
            if (mode.b == i) {
                return mode;
            }
        }

        return null;
    }
}