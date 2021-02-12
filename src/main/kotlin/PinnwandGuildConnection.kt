import command.CommandCallback
import db.*
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.message.*
import discord4j.core.spec.MessageCreateSpec
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.function.Predicate
import kotlin.math.min

class PinnwandGuildConnection(
    val discord: GatewayDiscordClient,
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

    val log = LoggerFactory.getLogger(PinnwandGuildConnection::class.java)

    var prefix: String = pinnwandGuild.commandPrefix
        set(value) {
            field = value
            transaction {
                pinnwandGuild.commandPrefix = prefix
            }
        }

    val pinboard = Pinboard(guild, pinnwandGuild.pinThreshold, guildChannel)

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
                transaction {
                    pinnwandGuild.pinboardChannel = pinboard.channel?.id?.asLong()
                }
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

        override fun rescan(limit: Int) {
            println("Scanning the pinboard's backlog")
            doRescan(limit)
        }

        override fun leaderboard(channelId: Snowflake, page: Int) {
            guild.getChannelById(channelId).subscribe { channel ->
                (channel as? GuildMessageChannel)?.let {
                    val leaderboard = Leaderboard.tally(guild.id)
                    val content = formatLeaderboard(leaderboard, 20, (page - 1) * 20)
                    log.info("Leaderboard: \n$content")
                    channel.createMessage { mcs ->
                        mcs.setEmbed {
                            it.setDescription("Pinnwand Leaderboard")
                            it.addField("#", content.substring(0, min(content.length, 1000)), true)
                        }
                    }.subscribe()
                }
            }
        }
    }

    val commandHandler = CommandHandler(commandCallback)

    fun addReact(event: ReactionAddEvent) {
        val emoji = event.emoji.normalise()
        val reactor = event.userId
        println("Added React: $emoji by ${reactor.mention()} on ${event.messageId.asLong()} ${if (emoji == pinEmoji) "PIN" else ""}")
        if (emoji == pinEmoji) {
            event.message.subscribe { message ->
                message.author.k?.id?.let {
                    pinboard.updateBasedOn(message, it, message.countPins())
                }
            }
        }
    }

    fun removeReact(event: ReactionRemoveEvent) {
        val emoji = event.emoji.normalise()
        val message = event.messageId
        val reactor = event.userId
        println("Removed React: $emoji by ${reactor.mention()} on ${message.asLong()} ${if (emoji == pinEmoji) "PIN" else ""}")
        if (emoji == pinEmoji) {
            event.message.subscribe { message ->
                message.author.k?.id?.let {
                    pinboard.updateBasedOn(message, it, message.countPins())
                }
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

    fun doRescan(limit: Int) {
        val channel = pinboard.channel ?: return
        val lastMessage = channel.lastMessageId.k ?: return
        channel.getMessagesBefore(lastMessage).filter {
            it.author.k?.id == discord.selfId
        }.take(limit.toLong()).map {
            print("Trying to extract from message: ${MessageURL(guild.id, channel.id, it.id)} ...")
            val result = PinboardScan.scan(guild.id, it)
            println("$result")
            result
        }.filter { it is PinboardScan.Success }.map { it as PinboardScan.Success }.collectList().subscribe { messages ->
            println("Found ${messages.size} messages")
            for (message in messages) {
                transaction {
                    val pinboardPostId = message.pinboardPost.message.asLong()
                    val originalId = message.originalPost.message.asLong()
                    if (PinboardMessage.findById(pinboardPostId) == null) {
                        //Delete duplicates
                        PinboardMessages.deleteWhere {
                            PinboardMessages.message eq originalId
                        }
                        PinboardMessage.new(pinboardPostId) {
                            this.message = DiscordMessage.findById(originalId) ?: DiscordMessage.new(originalId) {
                                this.pinCount = message.pinCount ?: pinThreshold
                                this.author = message.user.asLong()
                                this.guild = pinnwandGuild
                                this.channel = message.originalPost.channel.asLong()
                            }
                            this.guild = pinnwandGuild
                            this.channel = channel.id.asLong()
                        }
                    }
                }
            }
        }
    }

    private fun Message.countPins(): Int {
        return this.reactions.find { it.emoji.normalise() == pinEmoji }?.count ?: 0
    }
}