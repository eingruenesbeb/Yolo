/*
 * This program is a plugin for Minecraft Servers called "Yolo".
 * Copyright (C) 2023  eingruenesbeb
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * You can reach the original author via e-Mail: agreenbeb@gmail.com
 */
package io.github.eingruenesbeb.yolo.serialize

import io.github.eingruenesbeb.yolo.Yolo
import org.apache.commons.lang3.SerializationException
import org.apache.commons.lang3.SerializationUtils
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.io.BukkitObjectInputStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.Serializable

// Leave public, as other plugins may want to (de)serialize this ones data.

/**
 * This class represents a
 * [PersistentDataType](https://jd.papermc.io/paper/1.19/org/bukkit/persistence/PersistentDataContainer.html), that is
 * used to save an indexed array of [ItemStack](https://jd.papermc.io/paper/1.19/org/bukkit/inventory/ItemStack.html)s
 * to be saved to a
 * [PersistentDataContainer](https://jd.papermc.io/paper/1.19/org/bukkit/persistence/PersistentDataContainer.html).
 * Useful for storing data about an inventory.
 */
class ItemStackArrayPersistentDataType : PersistentDataType<Array<PersistentDataContainer>, Array<ItemStack?>> {
    private data class IndexedItemStackData(val index: Int, val itemStackData: ByteArray) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }

        fun convertDataToItemStack(): ItemStack {
            return ItemStack.deserializeBytes(itemStackData)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as IndexedItemStackData

            if (index != other.index) return false
            return itemStackData.contentEquals(other.itemStackData)
        }

        override fun hashCode(): Int {
            var result = index
            result = 31 * result + itemStackData.contentHashCode()
            return result
        }
    }

    private val contentsKey: NamespacedKey = NamespacedKey(JavaPlugin.getPlugin(Yolo::class.java), "contents")
    private val lengthKey: NamespacedKey = NamespacedKey(JavaPlugin.getPlugin(Yolo::class.java), "length")

    override fun getPrimitiveType(): Class<Array<PersistentDataContainer>> {
        return Array<PersistentDataContainer>::class.java
    }

    override fun getComplexType(): Class<Array<ItemStack?>> {
        return Array<ItemStack?>::class.java
    }

    override fun toPrimitive(
        complex: Array<ItemStack?>,
        context: PersistentDataAdapterContext
    ): Array<PersistentDataContainer> {
        var itemStackData = emptyArray<PersistentDataContainer>()

        // Data in a PDC of this type should always include the original length for later reconstruction.
        val lengthContainer = context.newPersistentDataContainer()
        lengthContainer.set(lengthKey, PersistentDataType.INTEGER, complex.size)
        itemStackData = itemStackData.plusElement(lengthContainer)

        for (i in complex.indices) {
            val indexedItemData = complex[i]?.let { SerializationUtils.serialize(IndexedItemStackData(i, it.serializeAsBytes())) }
            val container = context.newPersistentDataContainer()
            if (indexedItemData != null) {
                container.set(contentsKey, PersistentDataType.BYTE_ARRAY, indexedItemData)
                itemStackData = itemStackData.plusElement(container)
            }
        }
        return itemStackData
    }

    @Throws(SerializationException::class)
    override fun fromPrimitive(
        primitive: Array<PersistentDataContainer>,
        context: PersistentDataAdapterContext
    ): Array<ItemStack?> {
        // Again: Data that uses this type should also include information about the length of the inventory.
        // This data should be encoded in the first element of the array.
        // If that's the case, the element is simply being skipped.
        // In case the length is at another index, it's simply ignored (in order to cut down a bit on complexity.)
        val length = primitive.firstOrNull()?.get(lengthKey, PersistentDataType.INTEGER)
        var recoveredItemStacks = arrayOfNulls<ItemStack?>(length ?: primitive.size)

        for (i in primitive.indices) {
            if (i == 0 && length != null) continue
            val contents = primitive[i].get(contentsKey, PersistentDataType.BYTE_ARRAY) ?: continue
            try {
                val inputStream = BukkitObjectInputStream(ByteArrayInputStream(contents))
                val recoveredIndexedItemStack = inputStream.readObject() as? IndexedItemStackData
                if (recoveredIndexedItemStack != null) {
                    try {
                        recoveredItemStacks[recoveredIndexedItemStack.index] =
                            recoveredIndexedItemStack.convertDataToItemStack()
                    } catch (e: IndexOutOfBoundsException) {
                        recoveredItemStacks = recoveredItemStacks.copyOf(recoveredIndexedItemStack.index)
                    }
                }
            } catch (e: IOException) {
                // The PDC isn't accessible, so something must be terribly wrong...
                throw SerializationException("PDC is inaccessible!")
            } catch (e: ClassNotFoundException) {
                // Should never happen, as `IndexedItemData` is embedded.
                throw SerializationException("Challenge complete!\nHow did we get here?")
            }
        }

        return recoveredItemStacks
    }
}
