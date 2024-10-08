package de.miraculixx.maptools.utils.data

enum class Modules(val isSupporter: Boolean = false, val isExperimental: Boolean = false) {
    ANIMATION(true),
    BLOCK_UPDATE,
    COMMAND_TOOL,
    BLOCK_CONVERTER,
    HIT_BOX,
    LEASH,
    MARKER,
    MULTI_TOOL,
    NAME_TAG(true),
    MESSAGES,
    PATHING,
    QUEST_BOOK(isExperimental = true),
    TAG_TOOL,
    RESOURCE_PACK,
    POSITIONS,
    DANGER_WARNING,
    TEXT_STYLING,
    SYNC,

    CORE
}