package de.miraculixx.maptools.utils.extensions

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandTree

inline fun command(name: String, tree: CommandTree.() -> Unit = {}) = CommandTree(name).apply(tree)

fun CommandTree.unregister() {
    CommandAPI.unregister(name)
}