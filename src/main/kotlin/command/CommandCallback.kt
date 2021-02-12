package command

import discord4j.common.util.Snowflake
import discord4j.core.spec.MessageCreateSpec

interface CommandCallback {

    fun setPrefix(newPrefix: String)

    fun getPrefix(): String

    fun setPinEmoji(newEmoji: String)

    fun setPinboard(channel: Snowflake)

    fun sendMessage(channel: Snowflake, spec: MessageCreateSpec.() -> Unit)

    fun setThreshold(newThreshold: Int)

    fun rescan(limit: Int)

    fun leaderboard(channelId: Snowflake, page: Int)
}