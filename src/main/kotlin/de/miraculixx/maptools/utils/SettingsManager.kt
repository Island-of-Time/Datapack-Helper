package de.miraculixx.maptools.utils

import de.miraculixx.kpaper.extensions.console
import de.miraculixx.kpaper.extensions.kotlin.enumOf
import de.miraculixx.kpaper.localization.Config
import de.miraculixx.kpaper.localization.Localization
import de.miraculixx.maptools.command.*
import de.miraculixx.maptools.events.DangerWarningEvent
import de.miraculixx.maptools.events.TextInputEvent
import de.miraculixx.maptools.interfaces.DataHolder
import de.miraculixx.maptools.interfaces.Reloadable
import de.miraculixx.maptools.utils.data.Modules
import de.miraculixx.maptools.utils.data.PluginSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.scoreboard.Criteria
import java.io.File

object SettingsManager {
    val settingsFolder = File("plugins/MUtils/BuilderTools").apply { if (!exists()) mkdirs() }
    private val pluginSettingsFile = File(settingsFolder, "modules.json")
    var pluginSettings = PluginSettings()
    val packMeta = this::class.java.getResourceAsStream("/header/datapack.mcmeta")?.readAllBytes() ?: byteArrayOf()
    lateinit var localization: Localization
    private lateinit var config: Config
    private val moduleState: MutableMap<Modules, Boolean> = mutableMapOf()
    private val json = Json {
        encodeDefaults = true
        prettyPrint = true
    }

    var scoreboard: String = String()
        private set
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

    private val modules = mapOf(
        Modules.ANIMATION to AnimationCommand(),
        Modules.BLOCK_UPDATE to BlockUpdateCommand(),
        Modules.COMMAND_TOOL to CommandToolCommand(),
        Modules.BLOCK_CONVERTER to ConvertBlockCommand(),
        Modules.HIT_BOX to HitBoxCommand(),
        Modules.LEASH to LeashCommand(),
        Modules.MARKER to MarkerCommand(),
        Modules.MULTI_TOOL to MultiToolCommand(),
        Modules.NAME_TAG to NameTagCommand(),
        Modules.MESSAGES to NewMessage(),
        Modules.PATHING to PathingCommand(),
        Modules.QUEST_BOOK to QuestBookCommand(),
        Modules.TAG_TOOL to TagToolCommand(),
        Modules.RESOURCE_PACK to TexturePackCommand(),
        Modules.POSITIONS to WarpCommand(),
        Modules.DANGER_WARNING to DangerWarningEvent(),
        Modules.TEXT_STYLING to TextInputEvent(),
        Modules.SYNC to SyncCommand,
    )
    private val reloadable = modules.values.filterIsInstance<Reloadable>()
    private val dataHolder = modules.values.filterIsInstance<DataHolder>()

    fun reload() {
        if (!settingsFolder.exists()) settingsFolder.mkdirs()

        // plugin settings
        if (pluginSettingsFile.exists()) {
            try {
                val settings = json.decodeFromString<PluginSettings>(pluginSettingsFile.readText())
                settings.mainModules.forEach { (m, u) -> pluginSettings.mainModules[m] = u }
                settings.supporterModules.forEach { (m, u) -> pluginSettings.supporterModules[m] = u && tooling }
                settings.experimentalModules.forEach { (m, u) -> pluginSettings.supporterModules[m] = u }
            } catch (e: Exception) {
                console.sendMessage(prefix + cmp("Failed to read module configuration file! Reason: ${e.message}"))
            }
        }
        pluginSettings.mainModules.forEach { (t, u) -> if (moduleState[t] != u) changeModuleState(t, u) }
        pluginSettings.supporterModules.forEach { (t, u) -> if (moduleState[t] != (u && tooling)) changeModuleState(t, u) }
        pluginSettings.experimentalModules.forEach { (t, u) -> if (moduleState[t] != u) changeModuleState(t, u) }
        Modules.entries.forEach { m ->
            if (moduleState[m] == null) moduleState[m] = !m.isExperimental && (tooling || !m.isSupporter)
            if (!tooling && m.isSupporter) moduleState[m] = false
        }

        // language
        val languages = listOf("en_US", "de_DE").map { it to javaClass.getResourceAsStream("/language/$it.yml") }
        localization = Localization(File(settingsFolder, "language"), pluginSettings.language, languages)

        // module settings
        config = Config(javaClass.getResourceAsStream("/settings.yml"), "settings", File(settingsFolder, "settings.yml"))
        texturePackFolder = config.get<String>("texturepack-folder") ?: "texturepack"
        highlightGlobal = enumOf<Material>(config.getString("block-highlight.other")) ?: Material.YELLOW_CONCRETE
        highlightStairs = enumOf<Material>(config.getString("block-highlight.stairs")) ?: Material.BAMBOO_MOSAIC_STAIRS
        highlightWalls = enumOf<Material>(config.getString("block-highlight.walls")) ?: Material.END_STONE_BRICK_WALL
        highlightFence = enumOf<Material>(config.getString("block-highlight.fences")) ?: Material.BAMBOO_FENCE
        highlightSlabs = enumOf<Material>(config.getString("block-highlight.slabs")) ?: Material.BAMBOO_MOSAIC_SLAB
        groupFolders = config.getStringList("texturepack-groups").ifEmpty { listOf("general") }
        messageFolder = config.get<String>("datapack-names.messages") ?: "maptools.chat"
        messageLanguages = config.getStringList("message-languages").ifEmpty { listOf("english") }
        animationFolder = config.get<String>("datapack-names.animations") ?: "maptools.animations"
        pathingFolder = config.get<String>("datapack-names.pathing") ?: "maptools.pathing"
        highlightPinkGlass = config.getBoolean("show-pink-glass")

        scoreboard = config.get<String>("tick-scoreboard") ?: "maptools.tick"
        val scoreboardManager = Bukkit.getScoreboardManager().mainScoreboard
        scoreboardManager.getObjective(scoreboard) ?: scoreboardManager.registerNewObjective(scoreboard, Criteria.DUMMY, cmp("MapTools Ticker", cHighlight))

        reloadable.forEach { rl -> rl.reload() }
    }

    fun save() {
        dataHolder.forEach { it.save() }
        pluginSettingsFile.writeText(json.encodeToString(pluginSettings))
    }

    fun saveReadFile(file: File, fallbackPath: String): String {
        return if (!file.exists() || !tooling) {
            file.parentFile.mkdirs()
            val source = this::class.java.getResourceAsStream("/$fallbackPath") ?: return ""
            val bytes = source.readBytes()
            source.close()
            file.writeBytes(bytes)
            bytes.decodeToString()
        } else file.readText()
    }

    fun getModuleState(module: Modules): Boolean {
        return moduleState[module] ?: false
    }

    private fun changeModuleState(module: Modules, state: Boolean) {
        if (state) enableModule(module) else disableModule(module)
    }

    fun enableModule(module: Modules) {
        if (getModuleState(module) || (module.isSupporter && !tooling)) return
        modules[module]?.enable()
        moduleState[module] = true
    }

    fun disableModule(module: Modules) {
        if (!getModuleState(module)) return
        modules[module]?.disable()
        moduleState[module] = false
    }

    fun getLoadedLangs() = localization.getLoadedKeys()

    init {
        reload()
    }
}
