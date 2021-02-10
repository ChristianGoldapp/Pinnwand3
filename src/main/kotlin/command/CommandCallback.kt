package command

import discord4j.common.util.Snowflake
import discord4j.core.spec.MessageCreateSpec

interface CommandCallback {

    fun setPrefix(newPrefix: String)

    fun getPrefix(): String

    fun setPinboard(channel: Snowflake)

    fun sendMessage(channel: Snowflake, spec: MessageCreateSpec.() -> Unit)
}