package de.miraculixx.maptools.command

import de.miraculixx.kpaper.extensions.bukkit.addCommand
import de.miraculixx.kpaper.extensions.bukkit.addCopy
import de.miraculixx.kpaper.extensions.console
import de.miraculixx.kpaper.runnables.async
import de.miraculixx.kpaper.runnables.sync
import de.miraculixx.maptools.interfaces.DataHolder
import de.miraculixx.maptools.interfaces.Module
import de.miraculixx.maptools.interfaces.Reloadable
import de.miraculixx.maptools.utils.*
import de.miraculixx.maptools.utils.SettingsManager.settingsFolder
import de.miraculixx.maptools.utils.extensions.Vec3dSerializer
import de.miraculixx.maptools.utils.extensions.Vec3iSerializer
import de.miraculixx.maptools.utils.extensions.command
import de.miraculixx.maptools.utils.extensions.unregister
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.LocationType
import dev.jorel.commandapi.kotlindsl.*
import io.papermc.paper.adventure.AdventureComponent
import io.papermc.paper.math.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.network.protocol.game.DebugPackets
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.craftbukkit.CraftWorld
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.optionals.getOrNull
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object SyncCommand : Module, DataHolder {
    private val fileArea = File(settingsFolder, "modules/sync-areas.json")
    private val areas: MutableMap<String, Area> = mutableMapOf()

    private val entityFactory = Bukkit.getEntityFactory()
    private val regex = ",(Paper|Spigot|Bukkit)\\.[^:]+:\\[.*?\\](?=[,}])|,(Paper|Spigot|Bukkit)\\.[^:]+:[^,}]+"


    override fun disable() {
        command.unregister()
    }

    override fun enable() {
        command.register()
    }

    private val test = commandTree("test") {
        textArgument("name") {
            integerArgument("color") {
                playerExecutor { player, args ->
                    val name = args[0] as String
                    val color = args[1] as Int
                    val target = player.getTargetBlock(null, 10)
                    DebugPackets.sendGameTestAddMarker((target.world as CraftWorld).handle, BlockPos(target.x, target.y, target.z), name, color, 10_000)
                    player.sendMessage(prefix + cmp("GameTestMarker $name added!"))
                }
            }
        }
        literalArgument("entity-data") {
            literalArgument("get") {
                entitySelectorArgumentOneEntity("entity") {
                    anyExecutor { sender, args ->
                        val entity = args[0] as org.bukkit.entity.Entity
                        val entitySnapshot = entity.createSnapshot()
                        sender.sendMessage(prefix + cmp("Entity data: \n${entitySnapshot?.asString}").addCopy(entitySnapshot?.asString ?: ""))
                    }
                }
            }
            literalArgument("set") {
                locationArgument("location") {
                    greedyStringArgument("data") {
                        anyExecutor { sender, args ->
                            val location = args[0] as Location
                            val data = args[1] as String
                            val entitySnapshot = Bukkit.getEntityFactory().createEntitySnapshot(data)
                            entitySnapshot.createEntity(location)
                            sender.sendMessage(prefix + cmp("Entity data copied!"))
                        }
                    }
                }
            }
        }
    }

    @Suppress("UnstableApiUsage")
    private val command = command("sync") {
        withPermission("maptools.sync")

        literalArgument("create") {
            anyExecutor { sender, _ -> sender.sendMessage(prefix + cmp("Create a new square area as base for clone and syncing to multiple areas.")) }
            stringArgument("name") {
                locationArgument("pos1", LocationType.BLOCK_POSITION) {
                    locationArgument("pos2", LocationType.BLOCK_POSITION) {
                        anyExecutor { sender, args ->
                            val name = args[0] as String
                            val pos1 = args[1] as Location
                            val pos2 = args[2] as Location

                            if (pos1 == pos2) {
                                sender.sendMessage(prefix + cmp("Please provide two different positions!", cError))
                                return@anyExecutor
                            }

                            if (areas.containsKey(name)) {
                                sender.sendMessage(prefix + cmp("An area with this name already exists!", cError))
                                return@anyExecutor
                            }


                            val (minPos, maxPos) = sortPositions(pos1, pos2)
                            val subArea = SubArea(minPos, maxPos, pos1.world.name)

                            sender.sendMessage(prefix + cmp("Parsing initial commit..."))
                            val timeStart = System.currentTimeMillis()
                            async {
                                subArea.prepareCommit(sender, "Initial commit")
                                val timeEnd = System.currentTimeMillis()
                                sender.sendMessage(prefix + cmp("Initial commit parsed in ${(timeEnd - timeStart).milliseconds}"))

                                areas[name] = Area(mutableMapOf("main" to subArea))
                                sender.sendMessage(prefix + cmp("Area $name created!"))
                            }
                        }
                    }
                }
            }
        }

        literalArgument("commit") {
            literalArgument("push") {
                stringArgument("area") {
                    replaceSuggestions(ArgumentSuggestions.stringCollection { areas.keys })
                    stringArgument("subarea") {
                        suggestSubAreas(0)
                        greedyStringArgument("message") {
                            anyExecutor { sender, args ->
                                val areaName = args[0] as String
                                val subAreaName = args[1] as String
                                val commitMessage = args[2] as String

                                val area = areas[areaName] ?: run {
                                    sender.sendMessage(prefix + cmp("Area $areaName not found!", cError))
                                    return@anyExecutor
                                }
                                val subArea = area.subAreas[subAreaName] ?: run {
                                    sender.sendMessage(prefix + cmp("Subarea $subAreaName not found in $areaName!", cError))
                                    return@anyExecutor
                                }

                                val timeStart = System.currentTimeMillis()
                                if (!subArea.prepareCommit(sender, commitMessage)) return@anyExecutor
                                val timeEnd = System.currentTimeMillis()
                                sender.sendMessage(prefix + cmp("Commit pushed in ${(timeEnd - timeStart).milliseconds}!"))
                            }
                        }
                    }
                }
            }
            literalArgument("sync") {
                anyExecutor { sender, _ -> sender.sendMessage(prefix + cmp("Sync a commit to another subarea to only apply changes: <area> <from> <commit-id> <to>")) }
                stringArgument("area") {
                    replaceSuggestions(ArgumentSuggestions.stringCollection { areas.keys })
                    stringArgument("subarea") {
                        suggestSubAreas(0)
                        integerArgument("commitID") {
                            suggestCommitIDs(0, 1)
                            stringArgument("targetSubarea") {
                                suggestSubAreas(0)
                                anyExecutor { sender, args ->
                                    val areaName = args[0] as String
                                    val subAreaName = args[1] as String
                                    val commitID = args[2] as? Int
                                    val targetSubAreaName = args[3] as String

                                    val area = areas[areaName] ?: run {
                                        sender.sendMessage(prefix + cmp("Area $areaName not found!", cError))
                                        return@anyExecutor
                                    }
                                    val subArea = area.subAreas[subAreaName] ?: run {
                                        sender.sendMessage(prefix + cmp("Subarea $subAreaName not found in $areaName!", cError))
                                        return@anyExecutor
                                    }
                                    val targetSubArea = area.subAreas[targetSubAreaName] ?: run {
                                        sender.sendMessage(prefix + cmp("Subarea $targetSubAreaName not found in $areaName!", cError))
                                        return@anyExecutor
                                    }

                                    val commits = subArea.commits
                                    if (commitID == null || commitID !in commits.indices) {
                                        sender.sendMessage(prefix + cmp("Commit $commitID not found in $subAreaName!", cError))
                                        return@anyExecutor
                                    }
                                    val commit = commits[commitID]

                                    val bukkitWorld = Bukkit.getWorld(targetSubArea.world) ?: run {
                                        sender.sendMessage(prefix + cmp("World ${targetSubArea.world} not found!", cError))
                                        return@anyExecutor
                                    }

                                    val timeStart = System.currentTimeMillis()
                                    val newCommitChanges = moveChanges(subArea.pos1, targetSubArea.pos1, commit.blockChanges)

                                    targetSubArea.commits.add(Commit(timeStart, sender.name, "Synced from $subAreaName commit $commitID", newCommitChanges, EntityChanges()))
                                    targetSubArea.lastBlockTotal.putAll(newCommitChanges)
                                    newCommitChanges.forEach { (pos, blockData) ->
                                        val block = bukkitWorld.getBlockAt(pos.x, pos.y, pos.z)
                                        block.setBlockData(Bukkit.createBlockData(blockData.dataString), false)
                                    }
                                    val timeEnd = System.currentTimeMillis()

                                    sender.sendMessage(prefix + cmp("Commit $commitID synced to $targetSubAreaName in ${(timeEnd - timeStart).milliseconds}!"))
                                }
                            }
                        }
                    }
                }
            }
            literalArgument("rollback") {
                stringArgument("area") {
                    replaceSuggestions(ArgumentSuggestions.stringCollection { areas.keys })
                    textArgument("subarea") {
                        suggestSubAreas(0)
                        integerArgument("commitID") {
                            suggestCommitIDs(0, 1)
                            anyExecutor { sender, args ->
                                val areaName = args[0] as String
                                val subAreaName = args[1] as String
                                val commitID = args[2] as? Int

                                val area = areas[areaName] ?: run {
                                    sender.sendMessage(prefix + cmp("Area $areaName not found!", cError))
                                    return@anyExecutor
                                }
                                val subArea = area.subAreas[subAreaName] ?: run {
                                    sender.sendMessage(prefix + cmp("Subarea $subAreaName not found in $areaName!", cError))
                                    return@anyExecutor
                                }

                                val commits = subArea.commits
                                if (commitID == null || commitID !in commits.indices) {
                                    sender.sendMessage(prefix + cmp("Commit $commitID not found in $subAreaName!", cError))
                                    return@anyExecutor
                                }

                                val bukkitWorld = Bukkit.getWorld(subArea.world) ?: run {
                                    sender.sendMessage(prefix + cmp("World ${subArea.world} not found!", cError))
                                    return@anyExecutor
                                }

                                async {
                                    val startTime = System.currentTimeMillis()
                                    val changeMap = buildMap {
                                        subArea.commits.forEachIndexed { index, commit ->
                                            if (index > commitID) return@forEachIndexed
                                            putAll(commit.blockChanges)
                                        }
                                    }
                                    subArea.lastBlockTotal.putAll(changeMap)
                                    changeMap.forEach { (pos, blockDataString) ->
                                        val block = bukkitWorld.getBlockAt(pos.x, pos.y, pos.z)
                                        val blockData = Bukkit.createBlockData(blockDataString.dataString)
                                        if (blockData == block.blockData) return@forEach
                                        sync { block.setBlockData(blockData, false) }
                                    }
                                    subArea.commits.removeIf { subArea.commits.indexOf(it) > commitID }
                                    val endTime = System.currentTimeMillis()
                                    sender.sendMessage(prefix + cmp("Rollbacked to commit $commitID in ${(endTime - startTime).milliseconds}!"))
                                }
                            }
                        }
                    }
                }
            }
            literalArgument("visualize") {
                stringArgument("area") {
                    replaceSuggestions(ArgumentSuggestions.stringCollection { areas.keys })
                    stringArgument("subarea") {
                        suggestSubAreas(0)
                        integerArgument("commitID") {
                            suggestCommitIDs(0, 1)
                            booleanArgument("revert", true) {
                                playerExecutor { player, args ->
                                    val areaName = args[0] as String
                                    val subAreaName = args[1] as String
                                    val commitID = args[2] as? Int
                                    val revert = args.getOptional(3).getOrNull() as? Boolean ?: false

                                    val area = areas[areaName] ?: run {
                                        player.sendMessage(prefix + cmp("Area $areaName not found!", cError))
                                        return@playerExecutor
                                    }
                                    val subArea = area.subAreas[subAreaName] ?: run {
                                        player.sendMessage(prefix + cmp("Subarea $subAreaName not found in $areaName!", cError))
                                        return@playerExecutor
                                    }

                                    val commits = subArea.commits
                                    if (commitID == null || commitID !in commits.indices) {
                                        player.sendMessage(prefix + cmp("Commit $commitID not found in $subAreaName!", cError))
                                        return@playerExecutor
                                    }
                                    val commit = commits[commitID]

                                    CoroutineScope(Dispatchers.Default).launch {
                                        if (revert) {
                                            val changeMap = buildMap {
                                                subArea.lastBlockTotal.forEach { (pos, blockData) ->
                                                    put(Position.block(pos.x, pos.y, pos.z), Bukkit.createBlockData(blockData.dataString))
                                                }
                                            }
                                            player.sendMultiBlockChange(changeMap)
                                            return@launch
                                        }

                                        val emptyMap = buildMap {
                                            val from = subArea.pos1
                                            val to = subArea.pos2
                                            for (x in from.x..to.x) {
                                                for (y in from.y..to.y) {
                                                    for (z in from.z..to.z) {
                                                        put(Position.block(x, y, z), Bukkit.createBlockData(Material.AIR))
                                                    }
                                                }
                                            }
                                        }
                                        player.sendMultiBlockChange(emptyMap)

                                        delay(0.5.seconds)
                                        val changeMap = buildMap {
                                            commit.blockChanges.forEach { (pos, blockData) ->
                                                put(Position.block(pos.x, pos.y, pos.z), Bukkit.createBlockData(blockData.dataString))
                                            }
                                        }
                                        player.sendMultiBlockChange(changeMap)

                                        player.sendMessage(prefix + cmp("You now see only changes from commit $commitID in $subAreaName!"))
                                        player.sendMessage(
                                            prefix + cmp("(Click here to reset the visualization)")
                                                .addCommand("/sync commit visualize $areaName $subAreaName $commitID true")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        literalArgument("status") {
            stringArgument("area") {
                replaceSuggestions(ArgumentSuggestions.stringCollection { areas.keys })
                anyExecutor { sender, args ->
                    val areaName = args[0] as String
                    val area = areas[areaName] ?: run {
                        sender.sendMessage(prefix + cmp("Area $areaName not found!", cError))
                        return@anyExecutor
                    }

                    val subAreas = area.subAreas
                    sender.sendMessage(prefix + cmp("Area $areaName:"))
                    subAreas.forEach { (subAreaName, subArea) ->
                        sender.sendMessage(cmp("- Subarea $subAreaName:"))
                        subArea.printInfo(sender, "  -")
                    }
                }

                stringArgument("subarea") {
                    suggestSubAreas(0)
                    anyExecutor { sender, args ->
                        val areaName = args[0] as String
                        val subAreaName = args[1] as String
                        val area = areas[areaName] ?: run {
                            sender.sendMessage(prefix + cmp("Area $areaName not found!", cError))
                            return@anyExecutor
                        }

                        val subArea = area.subAreas[subAreaName] ?: run {
                            sender.sendMessage(prefix + cmp("Subarea $subAreaName not found in $areaName!", cError))
                            return@anyExecutor
                        }

                        sender.sendMessage(prefix + cmp("Subarea $subAreaName in $areaName:"))
                        subArea.printInfo(sender, "-")
                    }

                    integerArgument("commitID") {
                        suggestCommitIDs(0, 1)
                        anyExecutor { sender, args ->
                            val areaName = args[0] as String
                            val subAreaName = args[1] as String
                            val commitID = args[2] as? Int
                            val area = areas[areaName] ?: run {
                                sender.sendMessage(prefix + cmp("Area $areaName not found!", cError))
                                return@anyExecutor
                            }

                            val subArea = area.subAreas[subAreaName] ?: run {
                                sender.sendMessage(prefix + cmp("Subarea $subAreaName not found in $areaName!", cError))
                                return@anyExecutor
                            }
                            val commits = subArea.commits
                            if (commitID == null || commitID !in commits.indices) {
                                sender.sendMessage(prefix + cmp("Commit $commitID not found in $subAreaName!", cError))
                                return@anyExecutor
                            }
                            val commit = commits[commitID]

                            sender.sendMessage(prefix + cmp("Commit $commitID in $subAreaName:"))
                            sender.sendMessage(cmp("- Time: ${SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(commit.time))}"))
                            sender.sendMessage(cmp("- Author: ${commit.author}"))
                            sender.sendMessage(cmp("- Message: ${commit.message}"))
                            sender.sendMessage(cmp("- Changes: ${commit.blockChanges.size} - ${commit.entityChanges.added.size}"))
                        }
                    }
                }
            }
        }

        literalArgument("clone") {
            anyExecutor { sender, _ -> sender.sendMessage(prefix + cmp("Clone a new subarea from the main subarea")) }
            stringArgument("area") {
                replaceSuggestions(ArgumentSuggestions.stringCollection { areas.keys })
                stringArgument("newName") {
                    locationArgument("position", LocationType.BLOCK_POSITION) {
                        anyExecutor { sender, args ->
                            val name = args[0] as String
                            val newName = args[1] as String
                            val target = args[2] as Location

                            val area = areas[name] ?: run {
                                sender.sendMessage(prefix + cmp("Area $name not found!", cError))
                                return@anyExecutor
                            }

                            if (area.subAreas.containsKey(newName)) {
                                sender.sendMessage(prefix + cmp("A subarea with the name $newName already exists in $name!", cError))
                                return@anyExecutor
                            }

                            val mainSubArea = area.subAreas["main"] ?: run {
                                sender.sendMessage(prefix + cmp("Main subarea not found in $name! This area seems corrupted...", cError))
                                return@anyExecutor
                            }

                            async {
                                val timeStart = System.currentTimeMillis()
                                val sourceMin = mainSubArea.pos1
                                val targetMin = Vec3i(target.x.toInt(), target.y.toInt(), target.z.toInt())
                                val newChanges = moveChanges(sourceMin, targetMin, mainSubArea.lastBlockTotal).toMutableMap()
                                val sourceMax = mainSubArea.pos2
                                val targetMax = Vec3i(sourceMax.x - sourceMin.x + targetMin.x, sourceMax.y - sourceMin.y + targetMin.y, sourceMax.z - sourceMin.z + targetMin.z)

                                val initialCommit = Commit(timeStart, sender.name, "Cloned from main subarea", newChanges, EntityChanges())
                                val clone = SubArea(targetMin, targetMax, target.world.name, mutableListOf(initialCommit), newChanges)
                                area.subAreas[newName] = clone
                                val timeEnd = System.currentTimeMillis()

                                sender.sendMessage(prefix + cmp("Subarea $newName calculated in ${(timeEnd - timeStart).milliseconds}! Placing in world..."))

                                sync {
                                    val bukkitWorld = target.world
                                    newChanges.forEach { (pos, blockData) ->
                                        val block = bukkitWorld.getBlockAt(pos.x, pos.y, pos.z)
                                        block.setBlockData(Bukkit.createBlockData(blockData.dataString), false)
                                    }
                                    val timeEnd2 = System.currentTimeMillis()
                                    sender.sendMessage(prefix + cmp("Subarea $newName placed in world in ${(timeEnd2 - timeEnd).milliseconds}!"))
                                }
                            }
                        }
                    }
                }
            }
        }

        literalArgument("remove") {
            anyExecutor { sender, _ -> sender.sendMessage(prefix + cmp("Remove an area or subarea")) }
            stringArgument("area") {
                replaceSuggestions(ArgumentSuggestions.stringCollection { areas.keys })
                anyExecutor { sender, args ->
                    val areaName = args[0] as String
                    areas.remove(areaName)
                    sender.sendMessage(
                        prefix + cmp(
                            "Area $areaName removed! " +
                                    "All physical changes are still in the world, delete specific subareas to remove them from world."
                        )
                    )
                }

                stringArgument("subarea") {
                    suggestSubAreas(0)
                    booleanArgument("delete-area", true) {
                        anyExecutor { sender, args ->
                            val areaName = args[0] as String
                            val subAreaName = args[1] as String
                            val deleteArea = args.getOptional(2).getOrNull() as? Boolean ?: false
                            val area = areas[areaName] ?: run {
                                sender.sendMessage(prefix + cmp("Area $areaName not found!", cError))
                                return@anyExecutor
                            }

                            if (subAreaName == "main") {
                                sender.sendMessage(prefix + cmp("You can't remove the main subarea!", cError))
                                return@anyExecutor
                            }

                            val subArea = area.subAreas.remove(subAreaName)
                            if (subArea == null) {
                                sender.sendMessage(prefix + cmp("Subarea $subAreaName not found in $areaName!", cError))
                                return@anyExecutor
                            }

                            if (deleteArea) {
                                val bukkitWorld = Bukkit.getWorld(subArea.world) ?: run {
                                    sender.sendMessage(prefix + cmp("World ${subArea.world} not found!", cError))
                                    return@anyExecutor
                                }

                                subArea.lastBlockTotal.forEach { (pos, _) ->
                                    bukkitWorld.getBlockAt(pos.x, pos.y, pos.z).setBlockData(Bukkit.createBlockData(Material.AIR), false)
                                }
                                subArea.lastEntityTotal.forEach { entityData ->

                                }
                            }
                            sender.sendMessage(prefix + cmp("Subarea $subAreaName removed from $areaName!"))
                        }
                    }
                }
            }
        }
    }

    private fun sortPositions(pos1: Location, pos2: Location): Pair<Vec3i, Vec3i> {
        return Vec3i(
            min(pos1.x, pos2.x).toInt(), min(pos1.y, pos2.y).toInt(), min(pos1.z, pos2.z).toInt()
        ) to Vec3i(
            max(pos1.x, pos2.x).toInt(), max(pos1.y, pos2.y).toInt(), max(pos1.z, pos2.z).toInt()
        )
    }

    private fun moveChanges(sourceLocation: Vec3i, targetLocation: Vec3i, sourceChanges: Map<Vec3i, DataHolder>): Map<Vec3i, DataHolder> {
        return buildMap {
            sourceChanges.forEach { (pos, blockData) ->
                val newPos = Vec3i(pos.x - sourceLocation.x + targetLocation.x, pos.y - sourceLocation.y + targetLocation.y, pos.z - sourceLocation.z + targetLocation.z)
                put(newPos, blockData.copy())
            }
        }
    }

    private fun Argument<*>.suggestSubAreas(areaPosition: Int) {
        replaceSuggestions { info, builder ->
            val areaName = info.previousArgs[areaPosition] as String
            val area = areas[areaName]
            area?.subAreas?.forEach { (subAreaName, subArea) ->
                builder.suggest(subAreaName, AdventureComponent(cmp("Position: ${subArea.pos1} (${subArea.world})")))
            }
            builder.buildFuture()
        }
    }

    private fun Argument<*>.suggestCommitIDs(areaPosition: Int, subAreaPosition: Int) {
        replaceSuggestions { info, builder ->
            val areaName = info.previousArgs[areaPosition] as String
            val subAreaName = info.previousArgs[subAreaPosition] as String
            val area = areas[areaName]
            val subArea = area?.subAreas?.get(subAreaName)
            subArea?.commits?.forEachIndexed { index, commit ->
                builder.suggest(index.toString(), AdventureComponent(cmp("Message: ${commit.message}\nAuthor: ${commit.author}")))
            }
            builder.buildFuture()
        }
    }

    override fun save() {
        console.sendMessage(prefix + cmp("Saving areas..."))
        fileArea.writeText(Json.encodeToString(areas))
    }

    override fun load() {
        if (!fileArea.exists()) return kotlin.run { fileArea.createNewFile() }
        console.sendMessage(prefix + cmp("Loading areas..."))
        areas.clear()
        try {
            areas.putAll(json.decodeFromString(fileArea.readText()))
        } catch (e: Exception) {
            console.sendMessage(prefix + cmp("Failed to load areas! Reason: ${e.message}", cError))
        }
    }

    @Serializable
    private data class Area(
        val subAreas: MutableMap<String, SubArea> = mutableMapOf()
    )

    @Serializable
    private data class SubArea(
        val pos1: @Serializable(with = Vec3iSerializer::class) Vec3i,
        val pos2: @Serializable(with = Vec3iSerializer::class) Vec3i,
        val world: String,
        val commits: MutableList<Commit> = mutableListOf(),
        val lastBlockTotal: MutableMap<@Serializable(with = Vec3iSerializer::class) Vec3i, DataHolder> = mutableMapOf(),
        val lastEntityTotal: MutableSet<DataHolder> = mutableSetOf()
    ) {

        // Create a new commit based on the current state of the area
        fun prepareCommit(player: CommandSender, message: String): Boolean {
            val isInitial = lastBlockTotal.isEmpty()

            // Get the world
            val bukkitWorld = Bukkit.getWorld(world) ?: return run<Boolean> {
                player.sendMessage(prefix + cmp("World $world not found!"))
                false
            }

            val changes = buildMap {
                // Go through all blocks in the area
                for (x in pos1.x..pos2.x) {
                    for (y in pos1.y..pos2.y) {
                        for (z in pos1.z..pos2.z) {
                            val block = bukkitWorld.getBlockAt(x, y, z)
                            val blockData = block.blockData
                            val blockPos = Vec3i(x, y, z)
                            val liteBlockData = DataHolder(blockData.asString)

                            // Check if the block has changed. If it has, add it to the changes map and update the lastTotal map
                            if (isInitial || lastBlockTotal[blockPos] != liteBlockData) {
                                put(Vec3i(x, y, z), liteBlockData)
                                lastBlockTotal[blockPos] = liteBlockData
                            }
                        }
                    }
                }
            }

            val entityStatics = mutableSetOf<DataHolder>()
            val entityChanges = buildSet {
                val difference = pos2.subtract(pos1)
                val radius = Vec3(difference.x / 2.0, difference.y / 2.0, difference.z / 2.0)
                val center = Vec3(pos1.x + radius.x, pos1.y + radius.y, pos1.z + radius.z)
                val entities = bukkitWorld.getNearbyEntities(Location(bukkitWorld, center.x, center.y, center.z), radius.x, radius.y, radius.z)

                entities.forEach { entity ->
                    val entitySnapshot = entity.createSnapshot() ?: return@forEach
                    // Skip entities that can't be snapshotted like players

                    val entityPos = Vec3(entity.x, entity.y, entity.z)
                    val entityData = DataHolder(entitySnapshot.asString, entityPos)
                    if (isInitial || !lastEntityTotal.contains(entityData)) {
                        add(entityData)
                        lastEntityTotal.add(entityData)
                    } else {
                        // Entity didn't change, add it to the static entities
                        entityStatics.add(entityData)
                    }
                }
            }
            val removedEntities = lastEntityTotal - entityStatics - entityChanges


            // If there are no changes, don't create a new commit
            if (changes.isEmpty() && entityChanges.isEmpty() && removedEntities.isEmpty()) {
                player.sendMessage(prefix + cmp("No changes detected!"))
                return false
            }

            // Create a new commit
            commits.add(Commit(System.currentTimeMillis(), player.name, message, changes, EntityChanges(entityChanges, removedEntities)))
            return true
        }

        fun printInfo(sender: CommandSender, pre: String) {
            sender.sendMessage("$pre SubArea: $pos1 - $pos2")
            sender.sendMessage("$pre World: $world")
            sender.sendMessage("$pre Commits: ${commits.size}")
        }
    }

    @Serializable
    private data class Commit(
        val time: Long,
        val author: String,
        val message: String,
        val blockChanges: Map<@Serializable(with = Vec3iSerializer::class) Vec3i, DataHolder>,
        val entityChanges: EntityChanges
    )

    @Serializable
    private data class EntityChanges(
        val added: Set<DataHolder> = setOf(),
        val removed: Set<DataHolder> = setOf()
    )

    @Serializable
    private data class DataHolder(
        @SerialName("d") val dataString: String,
        @SerialName("p") val position: @Serializable(with = Vec3dSerializer::class) Vec3? = null
    )
}