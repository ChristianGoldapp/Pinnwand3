import db.DiscordMessage
import db.PinboardMessage
import db.PinboardMessages
import db.PinnwandGuild
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import reactor.core.publisher.Mono
import java.util.*

class Pinboard(val guild: Guild, initialThreshold: Int, initialChannel: GuildMessageChannel?) {

    var channel: GuildMessageChannel? = initialChannel
    var threshold: Int = initialThreshold

    fun updateBasedOn(original: Message, authorId: Snowflake, pinCount: Int) {
        LOG.trace("Updating based on message by ${authorId.mention()} with $pinCount pins")
        LOG.trace(original.toString())
        if (pinCount >= threshold) {
            shouldPin(original, authorId, pinCount)
        } else shouldUnpin(original.id, pinCount)
    }

    fun shouldPin(original: Message, authorId: Snowflake, pinCount: Int) {
        LOG.info("Should pin message ($pinCount pins) by ${authorId.mention()}")
        LOG.info(original.toString())
        val discordMessage = transaction {
            val id = original.id.asLong()
            //Either find DiscordMessage by ID or create new
            val discordMessage = DiscordMessage.findById(id)
                ?: DiscordMessage.new(id) {
                    LOG.info("Entering a new DiscordMessage into the DB")
                    guild = PinnwandGuild.findById(this@Pinboard.guild.id.asLong())!!
                    channel = original.channelId.asLong()
                    author = authorId.asLong()
                    LOG.info(this.toString())
                }
            //Update the pin count
            discordMessage.pinCount = pinCount
            discordMessage
        }
        if (channel != null) {
            updatePinboard(original, discordMessage, authorId, pinCount)
        } else {
            LOG.warn("Can't pin! There is no pinboard channel set!")
        }
    }

    fun updatePinboard(original: Message, dbEntry: DiscordMessage, authorId: Snowflake, pinCount: Int) {
        channel?.let { ch ->
            LOG.info("Updating the pinboard to reflect the message ${original.id} by $authorId with $pinCount pins")
            LOG.info("$dbEntry")
            transaction {
                val existingEntry = findExistingPinMessageInDB(dbEntry)
                //Three possible cases:
                val message = if (existingEntry != null) {
                    //Pinboard message has been created
                    val existingPinning = ch.findPinning(existingEntry)
                    val existingID = existingEntry.message.id.value
                    existingPinning.onErrorResume {
                        LOG.warn("The Pinboard message referencing $existingID has gone missing.")
                        LOG.warn("It will be re-created")
                        //Pinboard message has gone missing
                        createNewPinMessage(ch, original, authorId, pinCount)
                    }
                } else {
                    //Pinboard message has not been created
                    createNewPinMessage(ch, original, authorId, pinCount)
                }
                message.flatMap { it.update(original, authorId, pinCount) }.subscribe()
            }
        }
    }

    private fun Message.update(original: Message, authorId: Snowflake, pinCount: Int): Mono<Message> {
        val guildId = this@Pinboard.guild.id
        val link = MessageURL(guildId, original.channelId, original.id)
        val textContent = original.content.truncate(500).ifEmpty { "..." }
        val author = authorId.mention()
        val channel = original.channelId.channel()
        val imageUrl = original.extractImageURL()
        LOG.info("Binding message from $author in $channel")
        val pin = transaction { PinnwandGuild.findById(this@Pinboard.guild.id.asLong())!!.pinEmoji }

        return edit {
            it.setContent("A post from $author was pinned.")
            it.setEmbed { embed ->
                embed.setDescription("[Link to Post]($link)")
                embed.addField("Content", textContent, false)
                embed.addField("Author", author, true)
                embed.addField("Channel", channel, true)
                embed.setFooter("$pin $pinCount pushpins", null)
                imageUrl?.let { url -> embed.setImage(url) }
            }
        }
    }

    private fun findExistingPinMessageInDB(dbEntry: DiscordMessage): PinboardMessage? {
        return PinboardMessage.find {
            PinboardMessages.message eq dbEntry.id
        }.firstOrNull()
    }

    private fun createNewPinMessage(
        ch: GuildMessageChannel,
        original: Message,
        authorId: Snowflake,
        pins: Int
    ): Mono<Message> {
        val guildId = this@Pinboard.guild.id
        return ch.makePinMessage(original, authorId, pins).doOnSuccess { pinMessage ->
            val originalID = original.id.asLong()
            LOG.info("Creating a new Pin message based on $originalID by ${authorId.asLong()}")
            transaction {
                //Delete old messages before creating a new one
                val deletedOldMessages = PinboardMessages.deleteWhere { PinboardMessages.message eq originalID }
                if (deletedOldMessages > 0) {
                    LOG.warn("Database contained $deletedOldMessages stale references to message $originalID. They have been removed.")
                }
                val pinMsg = PinboardMessage.new(pinMessage.id.asLong()) {
                    this.channel = pinMessage.channelId.asLong()
                    this.guild = PinnwandGuild.findById(guildId.asLong())!!
                    this.message = DiscordMessage.findById(originalID)
                        ?: DiscordMessage.new(originalID) {
                            guild = PinnwandGuild.findById(this@Pinboard.guild.id.asLong())!!
                            channel = original.channelId.asLong()
                            author = authorId.asLong()
                            this.pinCount = pins
                        }
                }
                LOG.info(pinMsg.toString())
            }
        }
    }

    private fun GuildMessageChannel.findPinning(pinboardMessage: PinboardMessage): Mono<Message> {
        return getMessageById(Snowflake.of(pinboardMessage.id.value))
    }

    private fun GuildMessageChannel.makePinMessage(
        original: Message,
        authorId: Snowflake,
        pinCount: Int
    ): Mono<Message> {
        val guildId = this@Pinboard.guild.id
        val link = MessageURL(guildId, original.channelId, original.id)
        val textContent = original.content.truncate(500).ifEmpty { "..." }
        val author = authorId.mention()
        val channel = original.channelId.channel()
        val imageUrl = original.extractImageURL()
        LOG.info("Binding message from $author in $channel")
        val pin = transaction { PinnwandGuild.findById(this@Pinboard.guild.id.asLong())!!.pinEmoji }

        return createMessage {
            it.setContent("A post from $author was pinned.")
            it.setEmbed { embed ->
                embed.setDescription("[Link to Post]($link)")
                embed.addField("Content", textContent, false)
                embed.addField("Author", author, true)
                embed.addField("Channel", channel, true)
                embed.setFooter("$pin $pinCount pushpins", null)
                imageUrl?.let { url -> embed.setImage(url) }
            }
        }
    }

    fun shouldUnpin(originalId: Snowflake, pinCount: Int) {
        LOG.info("Should unpin ($pinCount pins): $originalId")
        transaction {
            val entry = DiscordMessage.findById(originalId.asLong())
            if (entry != null) {
                val pinEntry = PinboardMessage.find {
                    PinboardMessages.message eq entry.id
                }.firstOrNull()
                if (pinEntry != null) {
                    guild.getChannelById(Snowflake.of(pinEntry.channel)).flatMap {
                        (it as? GuildMessageChannel)?.let {
                            it.getMessageById(Snowflake.of(pinEntry.id.value))
                        } ?: Mono.empty()
                    }.flatMap {
                        it.delete()
                    }.doOnError {
                        LOG.error("An error occured while deleting message $originalId")
                        LOG.error(it.toString())
                    }.subscribe {
                        pinEntry.delete()
                    }
                }
                entry.delete()
            }
        }
    }
}