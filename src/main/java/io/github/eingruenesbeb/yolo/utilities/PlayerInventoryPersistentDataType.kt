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
package io.github.eingruenesbeb.yolo.utilities

import io.github.eingruenesbeb.yolo.Yolo
import org.apache.commons.lang3.SerializationException
import org.apache.commons.lang3.SerializationUtils
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.io.BukkitObjectInputStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.Serializable
import java.util.*

class PlayerInventoryPersistentDataType : PersistentDataType<Array<PersistentDataContainer>, PlayerInventory> {
    private data class IndexedItemStackData constructor(val index: Int, val itemStackData: ByteArray) : Serializable {
        fun convertDataToItemStack(): ItemStack {
            val inputStream = BukkitObjectInputStream(ByteArrayInputStream(itemStackData))
            return inputStream.readObject() as ItemStack
        }
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as IndexedItemStackData

            if (index != other.index) return false
            if (!itemStackData.contentEquals(other.itemStackData)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = index
            result = 31 * result + itemStackData.contentHashCode()
            return result
        }
    }

    private val contentsKey: NamespacedKey = NamespacedKey(JavaPlugin.getPlugin(Yolo::class.java), "contents")
    override fun getPrimitiveType(): Class<Array<PersistentDataContainer>> {
        return Array<PersistentDataContainer>::class.java
    }

    override fun getComplexType(): Class<PlayerInventory> {
        return PlayerInventory::class.java
    }

    override fun toPrimitive(
        complex: PlayerInventory,
        context: PersistentDataAdapterContext
    ): Array<PersistentDataContainer> {
        val contents = complex.contents
        var inventoryData = emptyArray<PersistentDataContainer>()
        for (i in contents.indices) {
            val indexedItemData = contents[i]?.let { SerializationUtils.serialize(IndexedItemStackData(i, it.serializeAsBytes())) }
            val container = context.newPersistentDataContainer()
            if (indexedItemData != null) {
                container.set(contentsKey, PersistentDataType.BYTE_ARRAY, indexedItemData)
                inventoryData = inventoryData.plusElement(container)
            }
        }
        return inventoryData
    }

    @Throws(SerializationException::class)
    override fun fromPrimitive(
        primitive: Array<PersistentDataContainer>,
        context: PersistentDataAdapterContext
    ): PlayerInventory {
        val toReturn = Bukkit.createInventory(null, InventoryType.PLAYER)
        val recoveredItemStacks = arrayOfNulls<ItemStack>(InventoryType.PLAYER.defaultSize)
        Arrays.stream(primitive).forEach { persistentDataContainer: PersistentDataContainer ->
            val contents = persistentDataContainer.get(contentsKey, PersistentDataType.BYTE_ARRAY)
            try {
                assert(contents != null)
                val inputStream = BukkitObjectInputStream(ByteArrayInputStream(contents))
                val recoveredIndexedItemStack = inputStream.readObject() as IndexedItemStackData
                recoveredItemStacks[recoveredIndexedItemStack.index] = recoveredIndexedItemStack.convertDataToItemStack()
            } catch (e: IOException) {
                throw SerializationException()
            } catch (e: ClassNotFoundException) {
                throw SerializationException()
            }
        }
        toReturn.contents = recoveredItemStacks
        return toReturn as PlayerInventory
    }
}
