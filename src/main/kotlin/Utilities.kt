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

fun Message.extractPostLink(): Pair<String, MessageURL?> {
    val embed = embeds.firstOrNull() ?: return "ERROR: No Embed" to null
    val description = embed.description.k ?: return "ERROR: No Description" to null
    val start = description.indexOf('(')
    val end = description.indexOf(')', start)
    if(start == -1 || end == -1) return "ERROR: Description has no markdown link: $description" to null
    val link = description.substring(start + 1, end)
    val prefix = "https://discordapp.com/channels/"
    if(!link.startsWith(prefix)) return "ERROR: Link has wrong form: $description" to null
    val params = link.substring(prefix.length, link.length).trim().split('/')
    if(params.size != 3) return return "ERROR: Link has wrong number of parameters ($params): $description" to null
    val (guild, channel, message) = params
    try {
        return link to MessageURL(Snowflake.of(guild), Snowflake.of(channel), Snowflake.of(message))
    } catch (ex: Exception){
        return "ERROR: Could not parse one of parameters ($params): $description" to null
    }
}

fun Snowflake.channel() = "<#${asString()}>"