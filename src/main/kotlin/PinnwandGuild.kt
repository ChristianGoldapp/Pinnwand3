import discord4j.core.`object`.entity.Guild
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.message.MessageDeleteEvent
import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.core.event.domain.message.ReactionRemoveEvent

class PinnwandGuild(guild: Guild) {
    fun addReact(event: ReactionAddEvent){

    }

    fun removeReact(event: ReactionRemoveEvent) {

    }

    fun createMessage(event: MessageCreateEvent) {

    }

    fun removeMessage(event: MessageDeleteEvent) {

    }
}