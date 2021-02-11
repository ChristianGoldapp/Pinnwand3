package db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

class Message(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Message>(Messages)

    var guild by PinnwandGuild referencedOn Messages.guild
    var channel by Messages.channel
}

object Messages : LongIdTable("message") {
    val guild = reference("guild", PinnwandGuilds)
    val channel = long("channel")
}