package com.willfp.eco.internal.gui.menu

import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.slot.FillerSlot
import com.willfp.eco.core.gui.slot.Slot
import com.willfp.eco.internal.gui.slot.EcoFillerSlot
import com.willfp.eco.util.StringUtils
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import java.util.function.Consumer

class EcoMenu(
    private val rows: Int,
    private val slots: List<MutableList<Slot>>,
    private val title: String,
    private val onClose: Consumer<InventoryCloseEvent>
): Menu {

    override fun getSlot(row: Int, column: Int): Slot {
        if (row < 1 || row > this.rows) {
            throw IllegalArgumentException("Invalid row number!")
        }

        if (column < 1 || column > 9) {
            throw IllegalArgumentException("Invalid column number!")
        }

        val slot = slots[row - 1][column - 1]
        if (slot is FillerSlot) {
            slots[row - 1][column - 1] = EcoFillerSlot(slot.itemStack)

            return getSlot(row, column)
        }

        return slot
    }

    override fun open(player: Player): Inventory {
        val inventory = Bukkit.createInventory(null, rows * 9, title)

        var i = 0
        for (row in slots) {
            for (item in row) {
                if (i == rows * 9) {
                    break
                }
                val slotItem = item.getItemStack(player)
                val meta = slotItem.itemMeta
                if (meta != null) {
                    val lore = meta.lore
                    if (lore != null) {
                        lore.replaceAll{ s -> StringUtils.format(s, player) }
                        meta.lore = lore
                    }
                    slotItem.itemMeta = meta
                }
                inventory.setItem(i, slotItem)
                i++
            }
        }

        player.openInventory(inventory)
        MenuHandler.registerMenu(inventory, this)
        return inventory
    }

    fun handleClose(event: InventoryCloseEvent) {
        onClose.accept(event)
    }

    override fun getRows(): Int {
        return rows
    }

    override fun getTitle(): String {
        return title
    }
}