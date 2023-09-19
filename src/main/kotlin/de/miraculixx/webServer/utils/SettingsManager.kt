package de.miraculixx.webServer.utils

import de.miraculixx.kpaper.extensions.kotlin.enumOf
import de.miraculixx.kpaper.localization.Config
import org.bukkit.Material
import java.io.File

class SettingsManager {
    val settingsFolder = File("plugins/MUtils/BuilderTools").apply { if (!exists()) mkdirs() }
    private lateinit var config: Config

    lateinit var texturePackFolder: String
        private set
    lateinit var highlightGlobal: Material
        private set
    lateinit var highlightStairs: Material
        private set
    lateinit var highlightWalls: Material
        private set
    lateinit var highlightFence: Material
        private set
    lateinit var highlightSlabs: Material
        private set
    lateinit var groupFolders: List<String>
        private set
    lateinit var messageFolder: String
        private set
    lateinit var messageLanguages: List<String>
        private set
    lateinit var animationFolder: String
        private set
    lateinit var pathingFolder: String
        private set
    var highlightPinkGlass: Boolean = false
        private set


    fun reload() {
        config = Config(javaClass.getResourceAsStream("/settings.yml"), "settings", File(settingsFolder, "settings.yml"))
        texturePackFolder = config.getString("texturepack-folder")
        highlightGlobal = enumOf<Material>(config.getString("block-highlight.other")) ?: Material.YELLOW_CONCRETE
        highlightStairs = enumOf<Material>(config.getString("block-highlight.stairs")) ?: Material.BAMBOO_MOSAIC_STAIRS
        highlightWalls = enumOf<Material>(config.getString("block-highlight.walls")) ?: Material.END_STONE_BRICK_WALL
        highlightFence = enumOf<Material>(config.getString("block-highlight.fences")) ?: Material.BAMBOO_FENCE
        highlightSlabs = enumOf<Material>(config.getString("block-highlight.slabs")) ?: Material.BAMBOO_MOSAIC_SLAB
        groupFolders = config.getStringList("texturepack-groups")
        messageFolder = config.getString("datapack-names.messages")
        messageLanguages = config.getStringList("message-languages")
        animationFolder = config.getString("datapack-names.animations")
        pathingFolder = config.getString("datapack-names.pathing")
        highlightPinkGlass = config.getBoolean("show-pink-glass")

    }

    init {
        reload()
    }
}
