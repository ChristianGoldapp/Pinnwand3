import db.DiscordMessages
import db.PinnwandGuilds
import discord4j.core.DiscordClient
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val db = Database.connect("jdbc:sqlite:test.sqlite")
    transaction(db) {
        SchemaUtils.create(PinnwandGuilds, DiscordMessages)
    }
    val token = File(args[0]).readText()

    val client = DiscordClient.create(token).login().doOnError {
        System.err.println("Could not connect Discord client")
        exitProcess(-1)
    }.block()!!
    val guildInit = GuildInitialization(client)

    guildInit.subscribe()

    client.onDisconnect().block()
}