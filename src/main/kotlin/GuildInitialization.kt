import discord4j.core.DiscordClient
import discord4j.core.`object`.entity.Guild
import discord4j.core.event.domain.lifecycle.ConnectEvent
import reactor.core.publisher.Flux

fun GuildInitialization(discord: DiscordClient): Flux<PinnwandGuild> {

    fun init(guild: Guild): PinnwandGuild {
        println("Connected to Guild ${guild.name}")
        //TODO: Register guild in database if it does not exist
        //TODO: Ensure that the pinboard channel exists if it is set and that we have the rights to use it
        //TODO: Initialize the Pinboard object for this guild here
        return PinnwandGuild(guild)
    }

    fun onConnect(connectEvent: ConnectEvent): Flux<PinnwandGuild> {
        val client = connectEvent.client
        return client.guilds.map { init(it) }
    }

    return discord.eventDispatcher.on(ConnectEvent::class.java).flatMap(::onConnect)
}