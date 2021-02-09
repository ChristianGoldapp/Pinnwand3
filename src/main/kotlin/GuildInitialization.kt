import discord4j.core.DiscordClient
import discord4j.core.event.domain.lifecycle.ConnectEvent

class GuildInitialization(discord: DiscordClient) {

    init {
        discord.eventDispatcher.on(ConnectEvent::class.java).subscribe(::onConnect)
    }

    fun onConnect(connectEvent: ConnectEvent){
        val client = connectEvent.client
        client.guilds.subscribe {
            println("Connected to Guild ${it.name}")
        }
    }
}