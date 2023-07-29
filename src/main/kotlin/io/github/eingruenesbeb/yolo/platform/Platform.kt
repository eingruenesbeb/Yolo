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

package io.github.eingruenesbeb.yolo.platform

import io.papermc.lib.PaperLib

abstract class Platform {
    val type = PaperLib.getEnvironment()
    val isFolia: Boolean = runCatching{
        if (PaperLib.isVersion(20)) {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
        } else {
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler")
        }
    }.isSuccess


}
