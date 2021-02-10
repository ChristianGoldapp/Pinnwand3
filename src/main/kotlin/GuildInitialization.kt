import db.PinnwandGuild
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.event.domain.lifecycle.ConnectEvent
import discord4j.rest.util.Permission
import org.jetbrains.exposed.sql.transactions.transaction
import reactor.core.publisher.Flux
import java.time.LocalDateTime

fun GuildInitialization(discord: GatewayDiscordClient): Flux<PinnwandGuildConnection> {

    val selfId = discord.selfId

    fun validatePinboardChannel(
        pinboardChannelId: Snowflake?,
        pinnwandGuild: PinnwandGuild,
        guild: Guild
    ) {
        if (pinboardChannelId != null) {
            fun onInvalid() {
                println("Channel $pinboardChannelId is not a valid channel. Either we cannot send messages in it or it does not exist.")
                pinnwandGuild.pinboardChannel = null
                pinnwandGuild.flush()
            }
            guild.getChannelById(pinboardChannelId).doOnError {
                onInvalid()
            }.subscribe {
                val channel = it as? GuildMessageChannel ?: return@subscribe onInvalid()
                channel.getEffectivePermissions(selfId).subscribe { permissions ->
                    if (!permissions.contains(Permission.SEND_MESSAGES)) {
                        onInvalid()
                    }
                }
            }
        }
    }

    fun registerGuild(guild: Guild) {
        transaction {
            val pinnwandGuild =
                PinnwandGuild.findById(guild.id.asLong()) ?: PinnwandGuild.new(guild.id.asLong()) {
                    firstJoined = LocalDateTime.now()
                }
            println("Settings for Guild: $pinnwandGuild")

            //Ensure that the pinboard channel exists if it is set and that we have the rights to use it
            val pinboardChannelId = pinnwandGuild.pinboardChannel?.let { Snowflake.of(it) }
            validatePinboardChannel(pinboardChannelId, pinnwandGuild, guild)
        }
    }

    fun init(guild: Guild): PinnwandGuildConnection {
        println("Connected to Guild ${guild.name}")

        //Register guild in database if it does not exist
        registerGuild(guild)
        return PinnwandGuildConnection(discord, guild)
    }

    fun onConnect(connectEvent: ConnectEvent): Flux<PinnwandGuildConnection> {
        val client = connectEvent.client
        return client.guilds.map { init(it) }
    }

    return discord.eventDispatcher.on(ConnectEvent::class.java).flatMap(::onConnect)
}