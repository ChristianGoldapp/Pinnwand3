package db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

class DiscordMessage(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<DiscordMessage>(DiscordMessages)

    var guild by PinnwandGuild referencedOn DiscordMessages.guild
    var channel by DiscordMessages.channel
    var author by DiscordMessages.author
    var pinCount by DiscordMessages.pinCount
}

object DiscordMessages : LongIdTable("message") {
    val guild = reference("guild", PinnwandGuilds)
    val channel = long("channel")
    val author = long("author")
    val pinCount = integer("pin_count")
}