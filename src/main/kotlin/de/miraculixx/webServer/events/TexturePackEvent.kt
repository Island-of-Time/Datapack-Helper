package de.miraculixx.webServer.events

import de.miraculixx.kpaper.event.listen
import de.miraculixx.mweb.api.MWebAPI
import de.miraculixx.webServer.utils.Zipping
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import java.io.File

object TexturePackEvent {
    private val tpFolder = File("texturepack")
    private val mixFolder = File(tpFolder, ".mixed")
    private val player1Folder = File(tpFolder, "player1")
    private val player2Folder = File(tpFolder, "player2")
    private val mwebAPI = MWebAPI.INSTANCE!!

    fun Player.loadPlayer1() {
        loadTP(player1Folder, this)
    }

    fun Player.loadPlayer2() {
        loadTP(player2Folder, this)
    }

    private fun loadTP(secondFolder: File, target: Player) {
        if (!secondFolder.exists()) secondFolder.mkdir()
        if (!mixFolder.exists()) mixFolder.mkdir()
        val zipTarget = File(mixFolder, "${secondFolder.name}.zip")
        if (zipTarget.exists()) zipTarget.delete()
        val folders = tpFolder.listFiles()?.filter { file ->
            val name = file.name
            !name.startsWith('.') &&
                    !name.startsWith("player")
        }?.toTypedArray() ?: emptyArray()
        Zipping.zipFolder(listOf(*folders, secondFolder), zipTarget)

        mwebAPI.sendFileAsResourcePack(zipTarget.path, setOf(target.uniqueId), false)
    }

    private val playerJoin = listen<PlayerJoinEvent> {
        val player = it.player
        val tags = player.scoreboardTags
        if (tags.contains("PLAYER_1")) player.loadPlayer1()
        else if (tags.contains("PLAYER_2")) player.loadPlayer2()
    }
}