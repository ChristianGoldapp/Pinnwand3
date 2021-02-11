import command.CommandCallback
import db.PinnwandGuild
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.message.*
import discord4j.core.spec.MessageCreateSpec
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.function.Predicate

class PinnwandGuildConnection(
    discord: GatewayDiscordClient,
    guildChannel: GuildMessageChannel?,
    val pinnwandGuild: PinnwandGuild,
    val guild: Guild
) {

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
    set(value){
        field = value
        transaction {
            pinnwandGuild.commandPrefix = prefix
        }
    }

    val pinboard = Pinboard(pinnwandGuild.pinThreshold, guildChannel)

    var pinEmoji: String = pinnwandGuild.pinEmoji
    set(value) {
        field = value
        transaction {
            pinnwandGuild.pinEmoji = pinEmoji
        }
    }

    var pinThreshold: Int = pinnwandGuild.pinThreshold
    set(value) {
        field = value
        pinboard.threshold = field
        transaction {
            pinnwandGuild.pinThreshold = pinThreshold
        }
    }

    val commandCallback = object : CommandCallback {

        override fun setPrefix(newPrefix: String) {
            prefix = newPrefix
            println("Setting new command prefix for ${guild.name}: $prefix")
        }

        override fun getPrefix(): String = prefix

        override fun setPinboard(channel: Snowflake) {
            guild.getChannelById(channel).subscribe {
                pinboard.channel = it as? GuildMessageChannel
                println("Set new pinboard channel: ${pinboard.channel?.name}")
            }
        }

        override fun sendMessage(channel: Snowflake, spec: MessageCreateSpec.() -> Unit) {
            guild.getChannelById(channel).subscribe {
                (it as? GuildMessageChannel ?: return@subscribe).createMessage(spec).subscribe()
            }
        }

        override fun setPinEmoji(newEmoji: String) {
            pinEmoji = newEmoji
            println("Setting new pinning emoji for ${guild.name}: $pinEmoji")
        }

        override fun setThreshold(newThreshold: Int) {
            pinThreshold = newThreshold
            println("Setting new pinning threshold for ${guild.name}: $pinThreshold")
        }
    }

    val commandHandler = CommandHandler(commandCallback)

    fun addReact(event: ReactionAddEvent) {
        val emoji = event.emoji.normalise()
        val message = event.messageId
        val reactor = event.userId
        println("Added React: $emoji by ${reactor.mention()} on ${message.asLong()} ${if(emoji == pinEmoji) "PIN" else ""}")
        if(emoji == pinEmoji){
            event.message.subscribe {
                pinboard.updateBasedOn(it, it.countPins())
            }
        }
    }

    fun removeReact(event: ReactionRemoveEvent) {
        val emoji = event.emoji.normalise()
        val message = event.messageId
        val reactor = event.userId
        println("Removed React: $emoji by ${reactor.mention()} on ${message.asLong()} ${if(emoji == pinEmoji) "PIN" else ""}")
        if(emoji == pinEmoji){
            event.message.subscribe {
                pinboard.updateBasedOn(it, it.countPins())
            }
        }
    }

    fun createMessage(event: MessageCreateEvent) {
        val message = event.message.content
        val author = event.member.k?.displayName ?: "<Unknown User>"
        println("Created message: by $author")
        println("\t$message")
        commandHandler.onMessage(event.message)
    }

    fun removeMessage(event: MessageDeleteEvent) {
        val message = event.message.k?.content ?: "<empty>"
        val author = event.message.k?.author?.k?.username ?: "<Unknown User>"
        pinboard.shouldUnpin(event.messageId, 0)
    }

    private fun Message.countPins(): Int{
        return this.reactions.find { it.emoji.normalise() == pinEmoji }?.count ?: 0
    }
}