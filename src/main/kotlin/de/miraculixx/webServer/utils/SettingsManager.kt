package de.miraculixx.webServer.utils

import de.miraculixx.kpaper.extensions.kotlin.enumOf
import de.miraculixx.kpaper.localization.Config
import de.miraculixx.webServer.command.*
import de.miraculixx.webServer.interfaces.DataHolder
import de.miraculixx.webServer.interfaces.Reloadable
import de.miraculixx.webServer.utils.data.PluginSettings
import org.bukkit.Material
import java.io.File

object SettingsManager {
    val settingsFolder = File("plugins/MUtils/BuilderTools").apply { if (!exists()) mkdirs() }
    val pluginSettings = PluginSettings()
    private lateinit var config: Config

    var texturePackFolder: String = String()
        private set
    var highlightGlobal: Material = Material.STONE
        private set
    var highlightStairs: Material = Material.STONE
        private set
    var highlightWalls: Material = Material.STONE
        private set
    var highlightFence: Material = Material.STONE
        private set
    var highlightSlabs: Material = Material.STONE
        private set
    var groupFolders: List<String> = emptyList()
        private set
    var messageFolder: String = String()
        private set
    var messageLanguages: List<String> = emptyList()
        private set
    var animationFolder: String = String()
        private set
    var pathingFolder: String = String()
        private set
    var highlightPinkGlass: Boolean = false
        private set

    private val commands = listOf(
        AnimationCommand,
        BlockUpdateCommand(),
        CommandToolCommand(),
        ConvertBlockCommand(),
        HitBoxCommand(),
        LeashCommand(),
        MarkerCommand(),
        MultiToolCommand(),
        NameTagCommand(),
        NewMessage(),
        OriginToolCommand(),
        PathingCommand(),
        QuestBookCommand(),
        ReloadDataPackCommand(),
        TagToolCommand(),
        TexturePackCommand(),
        WarpCommand()
    )
    private val reloadable = commands.filterIsInstance<Reloadable>()
    private val dataHolder = commands.filterIsInstance<DataHolder>()

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

        reloadable.forEach { rl -> rl.reload() }
    }

    fun save() {
        dataHolder.forEach { it.save() }
    }

    fun saveReadFile(file: File, fallbackPath: String): String {
        return if (!file.exists()) {
            file.parentFile.mkdirs()
            val source = javaClass::class.java.getResourceAsStream("/$fallbackPath") ?: return ""
            val bytes = source.readBytes()
            source.close()
            file.writeBytes(bytes)
            bytes.decodeToString()
        } else file.readText()
    }

    init {
        reload()
    }
}
