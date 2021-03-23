import db.PinnwandGuild
import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.channel.GuildMessageChannel
import discord4j.core.event.domain.lifecycle.ConnectEvent
import discord4j.rest.util.Permission
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

fun GuildInitialization(discord: GatewayDiscordClient): Flux<PinnwandGuildConnection> {

    val log = LoggerFactory.getLogger(PinnwandGuildConnection::class.java)
    val selfId = discord.selfId

    fun validatePinboardChannel(
        pinboardChannelId: Snowflake?,
        pinnwandGuild: PinnwandGuild,
        guild: Guild
    ) {
        if (pinboardChannelId != null) {
            fun onInvalid() {
                log.error("Channel $pinboardChannelId is not a valid channel. Either we cannot send messages in it or it does not exist.")
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

    fun registerGuild(guild: Guild): PinnwandGuild {
        return transaction {
            val pinnwandGuild =
                PinnwandGuild.findById(guild.id.asLong()) ?: PinnwandGuild.new(guild.id.asLong()) {
                    firstJoined = LocalDateTime.now()
                }
            log.info("Settings for Guild: $pinnwandGuild")

            //Ensure that the pinboard channel exists if it is set and that we have the rights to use it
            val pinboardChannelId = pinnwandGuild.pinboardChannel?.let { Snowflake.of(it) }
            validatePinboardChannel(pinboardChannelId, pinnwandGuild, guild)
            pinnwandGuild
        }
    }

    fun init(guild: Guild): Mono<PinnwandGuildConnection> {
        log.info("Connected to Guild ${guild.name}")

        //Register guild in database if it does not exist
        val pinnwandGuild = registerGuild(guild)
        return pinnwandGuild.pinboardChannel?.let {
            guild.getChannelById(Snowflake.of(it)).map { gc ->
                PinnwandGuildConnection(discord, gc as GuildMessageChannel, pinnwandGuild, guild)
            }
        } ?: Mono.just(PinnwandGuildConnection(discord, null, pinnwandGuild, guild))
    }

    fun onConnect(connectEvent: ConnectEvent): Flux<PinnwandGuildConnection> {
        val client = connectEvent.client
        return client.guilds.flatMap { init(it) }
    }

    return discord.eventDispatcher.on(ConnectEvent::class.java).flatMap(::onConnect)
}