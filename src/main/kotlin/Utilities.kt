import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.reaction.ReactionEmoji
import java.net.URL
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

fun String.markdownLink(url: String): String {
    return "[$this]($url)"
}

fun String.truncate(maxSize: Int, ending: Char = Typography.ellipsis): String {
    return if (this.length > maxSize) this.substring(0, maxSize) + ending
    else this
}

fun Message.extractImageURL(): String? {
    val raw = attachments.toList().getOrNull(0)?.url ?: embeds.getOrNull(0)?.url?.k ?: return null
    return try {
        URL(raw).toString()
    } catch (ex: Exception){
        println("Could not parse $raw as URL")
        null
    }
}

fun Snowflake.channel() = "<#${asString()}>"