import discord4j.common.util.Snowflake
import discord4j.core.`object`.reaction.ReactionEmoji
import java.util.*

val <T> Optional<T>.k get() = if (this.isPresent) this.get() else null

val <T> T?.o get() = Optional.ofNullable(this)

fun Snowflake.mention() = "<@${this.asLong()}>"

fun ReactionEmoji.normalise(): String {
    return asUnicodeEmoji().k?.raw ?: asCustomEmoji().k!!.normalise()
}

fun ReactionEmoji.Custom.normalise(): String {
    return "<:${name}:${id.asLong()}>"
}

fun String.stripColons(): String {
    return if (this[0] == ':') this.substring(1, this.length - 1)
    else this
}