import command.CommandCallback
import db.PinnwandGuild
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.channel.GuildChannel
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.message.*
import discord4j.core.spec.MessageCreateSpec
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.function.Predicate

class PinnwandGuildConnection(discord: GatewayDiscordClient, guildChannel: GuildMessageChannel?, val pinnwandGuild: PinnwandGuild, val guild: Guild) {

    init {
        fun <T : Event> subscribe(clazz: Class<T>, pred: Predicate<T>, callback: (T) -> Unit) {
            discord.on(clazz).filter(pred).subscribe(callback)
        }
        subscribe(ReactionAddEvent::class.java, { it.guildId.k == guild.id }, ::addReact)
        subscribe(ReactionRemoveEvent::class.java, { it.guildId.k == guild.id }, ::removeReact)
        subscribe(MessageCreateEvent::class.java, { it.guildId.k == guild.id }, ::createMessage)
        subscribe(MessageDeleteEvent::class.java, { true }, ::removeMessage)
    }

    var prefix: String = pinnwandGuild.commandPrefix
    var pinboard: GuildMessageChannel? = guildChannel

    val commandCallback = object : CommandCallback{
        override fun setPrefix(newPrefix: String) {
            prefix = newPrefix
            transaction {
                pinnwandGuild.commandPrefix = prefix
            }
            println("Setting new command prefix for ${guild.name}: $prefix")
        }

        override fun getPrefix(): String = prefix

        override fun setPinboard(channel: Snowflake) {
            guild.getChannelById(channel).subscribe {
                pinboard = it as? GuildMessageChannel
                println("Set new pinboard channel: ${pinboard?.name}")
            }
        }

        override fun sendMessage(channel: Snowflake, spec: MessageCreateSpec.() -> Unit) {
            guild.getChannelById(channel).subscribe {
                (it as? GuildMessageChannel ?: return@subscribe).createMessage(spec).subscribe()
            }
        }
    }

    val commandHandler = CommandHandler(commandCallback)

    fun addReact(event: ReactionAddEvent) {
        val emoji = event.emoji.asCustomEmoji()
        val message = event.messageId
        val reactor = event.userId
    }

    fun removeReact(event: ReactionRemoveEvent) {
        val emoji = event.emoji.asCustomEmoji()
        val message = event.messageId
        val reactor = event.userId
    }

    fun createMessage(event: MessageCreateEvent) {
        val message = event.message.content
        val author = event.member.k?.displayName ?: "<Unknown User>"
        commandHandler.onMessage(event.message)
    }

    fun removeMessage(event: MessageDeleteEvent) {
        val message = event.message.k?.content ?: "<empty>"
        val author = event.message.k?.author?.k?.username ?: "<Unknown User>"
    }
}