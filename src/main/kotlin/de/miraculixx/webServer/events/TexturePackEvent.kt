@file:Suppress("unused")

package de.miraculixx.webServer.events

import de.miraculixx.kpaper.event.listen
import de.miraculixx.kpaper.runnables.taskRunLater
import de.miraculixx.webServer.Main
import de.miraculixx.webServer.settings
import de.miraculixx.webServer.utils.SettingsManager.groupFolders
import de.miraculixx.webServer.utils.SettingsManager.texturePackFolder
import de.miraculixx.webServer.utils.Zipping
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import java.io.File

object TexturePackEvent {
    private val mWebAPI = if (Main.mWebLoaded) Main.mWebAPI else null

    fun loadTP(secondFolderName: String, target: Player, groups: List<String>) {
        val tpFolder = File(texturePackFolder)
        val mixFolder = File(tpFolder, ".mixed")
        val secondFolder = File(tpFolder, secondFolderName)
        if (!secondFolder.exists()) secondFolder.mkdir()
        if (!mixFolder.exists()) mixFolder.mkdir()
        val zipTarget = File(mixFolder, "${secondFolder.name}.zip")
        if (zipTarget.exists()) zipTarget.delete()
        val folders = tpFolder.listFiles()?.filter { file ->
            val name = file.name
            !name.startsWith('.') && !groups.contains(name)
        }?.toTypedArray() ?: emptyArray()
        Zipping.zipFolder(listOf(*folders, secondFolder), zipTarget)

        mWebAPI?.sendFileAsResourcePack(zipTarget.path, setOf(target.uniqueId), false)
    }

    private val playerJoin = listen<PlayerJoinEvent> {
        if (mWebAPI == null) return@listen
        val player = it.player
        taskRunLater(20) {
            val groups = groupFolders
            player.scoreboardTags.forEach { tag ->
                if (!groups.contains(tag)) return@forEach
                loadTP(tag, player, groups)
                return@taskRunLater
            }
        }
    }
}