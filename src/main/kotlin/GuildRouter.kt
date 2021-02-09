import discord4j.core.DiscordClient
import discord4j.core.`object`.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.MessageDeleteEvent
import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.core.event.domain.message.ReactionRemoveEvent

class GuildRouter(discord: DiscordClient, val guilds: Map<Snowflake, PinnwandGuild>) {

    init {
        discord.eventDispatcher.on(ReactionAddEvent::class.java).subscribe(::addReact)
        discord.eventDispatcher.on(ReactionRemoveEvent::class.java).subscribe(::removeReact)
        discord.eventDispatcher.on(MessageCreateEvent::class.java).subscribe(::createMessage)
        discord.eventDispatcher.on(MessageDeleteEvent::class.java).subscribe(::removeMessage)
    }

    fun addReact(event: ReactionAddEvent) {
        event.guildId.k?.let { onGuild(it) { this.addReact(event) } }
    }

    fun removeReact(event: ReactionRemoveEvent) {
        event.guildId.k?.let { onGuild(it) { this.removeReact(event) } }
    }

    fun createMessage(event: MessageCreateEvent) {
        event.guildId.k?.let { onGuild(it) { this.createMessage(event) } }
    }

    fun removeMessage(event: MessageDeleteEvent) {
        onAllGuilds { this.removeMessage(event) }
    }

    fun onAllGuilds(closure: PinnwandGuild.() -> Unit) = guilds.forEach { it.value.closure() }

    fun onGuild(snowflake: Snowflake?, closure: PinnwandGuild.() -> Unit) = guilds[snowflake]?.let { it.closure() }
}