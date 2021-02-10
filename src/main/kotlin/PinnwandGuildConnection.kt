import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Guild
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.message.*
import java.util.function.Predicate

class PinnwandGuildConnection(discord: GatewayDiscordClient, val guild: Guild) {

    init {
        fun <T : Event> subscribe(clazz: Class<T>, pred: Predicate<T>, callback: (T) -> Unit) {
            discord.on(clazz).filter(pred).subscribe(callback)
        }
        subscribe(ReactionAddEvent::class.java, { it.guildId.k == guild.id }, ::addReact)
        subscribe(ReactionRemoveEvent::class.java, { it.guildId.k == guild.id }, ::removeReact)
        subscribe(MessageCreateEvent::class.java, { it.guildId.k == guild.id }, ::createMessage)
        subscribe(MessageDeleteEvent::class.java, { true }, ::removeMessage)
    }

    fun addReact(event: ReactionAddEvent) {
        val emoji = event.emoji.asCustomEmoji()
        val message = event.messageId
        val reactor = event.userId
        println("Added react in ${guild.name}: $emoji by $reactor on $message")
    }

    fun removeReact(event: ReactionRemoveEvent) {
        val emoji = event.emoji.asCustomEmoji()
        val message = event.messageId
        val reactor = event.userId
        println("Added react in ${guild.name}: $emoji by $reactor on $message")
    }

    fun createMessage(event: MessageCreateEvent) {
        val message = event.message.content
        val author = event.member.k?.displayName ?: "<Unknown User>"
        println("Created message in ${guild.name} by $author")
        println(message)
    }

    fun removeMessage(event: MessageDeleteEvent) {
        val message = event.message.k?.content ?: "<empty>"
        val author = event.message.k?.author?.k?.username ?: "<Unknown User>"
        println("Deleted message in ${guild.name} by by $author")
        println(message)
    }
}