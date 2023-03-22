/*
 * This is program is a plugin for Minecraft Servers called "Yolo".
 * Copyright (c) 2023  eingruenesbeb
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *   You can reach the original author via e-Mail: agreenbeb@gmail.com
 */

package io.github.eingruenesbeb.yolo.utilities;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class BooleanPersistentDataType implements PersistentDataType<Boolean, Boolean> {
    @Override
    public @NotNull Class<Boolean> getPrimitiveType() {
        return boolean.class;
    }

    @Override
    public @NotNull Class<Boolean> getComplexType() {
        return boolean.class;
    }

    @Override
    public @NotNull Boolean toPrimitive(@NotNull Boolean complex, @NotNull PersistentDataAdapterContext context) {
        return complex;
    }

    @Override
    public @NotNull Boolean fromPrimitive(@NotNull Boolean primitive, @NotNull PersistentDataAdapterContext context) {
        return primitive;
    }
}