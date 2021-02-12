import discord4j.common.util.Snowflake
import discord4j.core.`object`.Embed
import discord4j.core.`object`.entity.Message

sealed class PinboardScan {
    data class Success(val pinboardPost: MessageURL, val originalPost: MessageURL, val user: Snowflake, val pinCount: Int? = null) : PinboardScan()
    data class Failure(val error: String, val data: String) : PinboardScan()

    companion object {
        fun scan(guildId: Snowflake, message: Message): PinboardScan {
            val embed = message.embeds.firstOrNull() ?: return Failure("ERROR: No Embed", message.toString())
            val description = embed.description.k ?: return Failure("ERROR: No Description", message.toString())
            val author = parseAuthor(embed) ?: return Failure("ERROR: Could not parse Author", message.toString())
            val link = parseLink(author, MessageURL(guildId, message.channelId, message.id), description)
            return if (link is Success) {
                link.copy(user = author, pinCount = parsePinCount(embed))
            } else link
        }

        private fun parseLink(user: Snowflake, pinboardPost: MessageURL, description: String): PinboardScan {
            val start = description.indexOf('(')
            val end = description.indexOf(')', start)
            if (start == -1 || end == -1) Failure("ERROR: Description has no markdown link", description)
            val link = description.substring(start + 1, end)
            val prefix = "https://discordapp.com/channels/"
            if (!link.startsWith(prefix)) return Failure("ERROR: Link has wrong form", description)
            val params = link.substring(prefix.length, link.length).trim().split('/')
            if (params.size != 3) return Failure("ERROR: Link has wrong number of parameters ($params)", description)
            val (g, c, m) = params
            try {
                return Success(pinboardPost, MessageURL(Snowflake.of(g), Snowflake.of(c), Snowflake.of(m)), user)
            } catch (ex: Exception) {
                return Failure("ERROR: Could not parse one of parameters ($params)", description)
            }
        }

        private fun parseAuthor(embed: Embed): Snowflake? {
            val mention = embed.fields.find { it.name == "Author" }?.value
            return mention?.parseMention()
        }

        private fun parsePinCount(embed: Embed): Int? {
            return try {
                val footer = embed.footer.k ?: return null
                val (_, pins, _) = footer.text.split(" ")
                pins.toIntOrNull()
            } catch (ex: Exception) {
                null
            }
        }
    }
}
