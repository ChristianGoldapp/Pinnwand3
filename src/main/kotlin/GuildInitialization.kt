import db.PinnwandGuild
import discord4j.core.DiscordClient
import discord4j.core.`object`.entity.Guild
import discord4j.core.event.domain.lifecycle.ConnectEvent
import org.jetbrains.exposed.sql.transactions.transaction
import reactor.core.publisher.Flux
import java.time.LocalDateTime

fun GuildInitialization(discord: DiscordClient): Flux<PinnwandGuildConnection> {

    fun init(guild: Guild): PinnwandGuildConnection {
        println("Connected to Guild ${guild.name}")
        //Register guild in database if it does not exist
        transaction {
            val pinnwandGuild = PinnwandGuild.findById(guild.id.asLong()) ?: PinnwandGuild.new(guild.id.asLong()){
                firstJoined = LocalDateTime.now()
            }
            println("Settings for Guild: $pinnwandGuild")
        }
        //TODO: Ensure that the pinboard channel exists if it is set and that we have the rights to use it
        //TODO: Initialize the Pinboard object for this guild here
        return PinnwandGuildConnection(discord, guild)
    }

    fun onConnect(connectEvent: ConnectEvent): Flux<PinnwandGuildConnection> {
        val client = connectEvent.client
        return client.guilds.map { init(it) }
    }

    return discord.eventDispatcher.on(ConnectEvent::class.java).flatMap(::onConnect)
}