import discord4j.core.DiscordClient
import discord4j.core.`object`.entity.Guild
import discord4j.core.event.domain.lifecycle.ConnectEvent

class GuildInitialization(discord: DiscordClient) {

    init {
        discord.eventDispatcher.on(ConnectEvent::class.java).subscribe(::onConnect)
    }

    fun onConnect(connectEvent: ConnectEvent){
        val client = connectEvent.client
        client.guilds.subscribe(::init)
    }

    fun init(guild: Guild){
        println("Connected to Guild ${guild.name}")
        //TODO: Register guild in database if it does not exist
        //TODO: Ensure that the pinboard channel exists if it is set and that we have the rights to use it
        //TODO: Initialize the Pinboard object for this guild here
    }
}