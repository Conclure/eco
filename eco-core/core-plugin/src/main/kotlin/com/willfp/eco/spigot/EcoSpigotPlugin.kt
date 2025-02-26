package com.willfp.eco.spigot

import com.willfp.eco.core.AbstractPacketAdapter
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.display.Display
import com.willfp.eco.core.integrations.IntegrationLoader
import com.willfp.eco.core.integrations.anticheat.AnticheatManager
import com.willfp.eco.core.integrations.antigrief.AntigriefManager
import com.willfp.eco.core.integrations.customitems.CustomItemsManager
import com.willfp.eco.core.integrations.mcmmo.McmmoManager
import com.willfp.eco.internal.drops.DropManager
import com.willfp.eco.proxy.BlockBreakProxy
import com.willfp.eco.proxy.FastItemStackFactoryProxy
import com.willfp.eco.proxy.SkullProxy
import com.willfp.eco.spigot.arrows.ArrowDataListener
import com.willfp.eco.spigot.display.*
import com.willfp.eco.spigot.drops.CollatedRunnable
import com.willfp.eco.spigot.eventlisteners.*
import com.willfp.eco.spigot.gui.GUIListener
import com.willfp.eco.spigot.integrations.anticheat.*
import com.willfp.eco.spigot.integrations.antigrief.*
import com.willfp.eco.spigot.integrations.customitems.CustomItemsOraxen
import com.willfp.eco.spigot.integrations.mcmmo.McmmoIntegrationImpl
import com.willfp.eco.spigot.recipes.ShapedRecipeListener
import com.willfp.eco.util.BlockUtils
import com.willfp.eco.util.SkullUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

abstract class EcoSpigotPlugin : EcoPlugin(
    87955,
    10043,
    "com.willfp.eco.proxy",
    "&a"
) {
    init {
        Display.setFinalizeKey(namespacedKeyFactory.create("finalized"))

        val skullProxy = getProxy(SkullProxy::class.java)
        SkullUtils.initialize { meta: SkullMeta, base64: String -> skullProxy.setSkullTexture(meta, base64) }

        val blockBreakProxy = getProxy(BlockBreakProxy::class.java)
        BlockUtils.initialize { player: Player, block: Block -> blockBreakProxy.breakBlock(player, block) }
    }

    override fun handleEnable() {
        CollatedRunnable(this)

        this.logger.info("Ignore messages about deprecated events!")

        if (!this.configYml.getBool("enable-bstats")) {
            logger.severe("")
            logger.severe("----------------------------")
            logger.severe("")
            logger.severe("Looks like you've disabled bStats!")
            logger.severe("This means that information about java version,")
            logger.severe("player count, server version, and other data")
            logger.severe("isn't able to be used to ensure that support isn't dropped!")
            logger.severe("Enable bStats in /plugins/eco/config.yml")
            logger.severe("")
            logger.severe("----------------------------")
            logger.severe("")
        }

        this.getProxy(FastItemStackFactoryProxy::class.java).create(ItemStack(Material.AIR)).unwrap()
    }

    override fun handleReload() {
        CollatedRunnable(this)
        DropManager.update(this)
    }

    override fun handleAfterLoad() {
        CustomItemsManager.registerAllItems()
    }

    override fun loadIntegrationLoaders(): List<IntegrationLoader> {
        return listOf(
            // AntiGrief
            IntegrationLoader("WorldGuard") { AntigriefManager.register(AntigriefWorldGuard()) },
            IntegrationLoader("GriefPrevention") { AntigriefManager.register(AntigriefGriefPrevention()) },
            IntegrationLoader("FactionsUUID") { AntigriefManager.register(AntigriefFactionsUUID()) },
            IntegrationLoader("Towny") { AntigriefManager.register(AntigriefTowny()) },
            IntegrationLoader("Lands") { AntigriefManager.register(AntigriefLands(this)) },
            IntegrationLoader("Kingdoms") { AntigriefManager.register(AntigriefKingdoms()) },
            IntegrationLoader("CombatLogX") {
                val pluginManager = Bukkit.getPluginManager()
                val combatLogXPlugin = pluginManager.getPlugin("CombatLogX") ?: return@IntegrationLoader
                val pluginVersion = combatLogXPlugin.description.version
                if (pluginVersion.startsWith("10")) {
                    AntigriefManager.register(AntigriefCombatLogXV10())
                }
                if (pluginVersion.startsWith("11")) {
                    AntigriefManager.register(AntigriefCombatLogXV11())
                }
            },

            // Anticheat
            IntegrationLoader("AAC5") { AnticheatManager.register(this, AnticheatAAC()) },
            IntegrationLoader("Matrix") { AnticheatManager.register(this, AnticheatMatrix()) },
            IntegrationLoader("NoCheatPlus") { AnticheatManager.register(this, AnticheatNCP()) },
            IntegrationLoader("Spartan") { AnticheatManager.register(this, AnticheatSpartan()) },
            IntegrationLoader("Vulcan") { AnticheatManager.register(this, AnticheatVulcan()) },

            // Custom Items
            IntegrationLoader("Oraxen") { CustomItemsManager.register(CustomItemsOraxen()) },

            // Misc
            IntegrationLoader("mcMMO") { McmmoManager.register(McmmoIntegrationImpl()) }
        )
    }

    override fun loadPacketAdapters(): List<AbstractPacketAdapter> {
        val adapters = mutableListOf(
            PacketAutoRecipe(this),
            PacketChat(this),
            PacketSetCreativeSlot(this),
            PacketSetSlot(this),
            PacketWindowItems(this)
        )

        if (!configYml.getBool("disable-display-on-villagers")) {
            adapters.add(PacketOpenWindowMerchant(this))
        }

        return adapters
    }

    override fun loadListeners(): List<Listener> {
        return listOf(
            NaturalExpGainListeners(),
            ArmorListener(),
            DispenserArmorListener(),
            EntityDeathByEntityListeners(this),
            ShapedRecipeListener(this),
            PlayerJumpListeners(),
            GUIListener(this),
            ArrowDataListener(this),
            ArmorChangeEventListeners(this)
        )
    }
}