import db.DBConfig
import db.DiscordMessages
import db.PinboardMessages
import db.PinnwandGuilds
import discord4j.core.DiscordClient
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val db = DBConfig.init(File(args[1])).connect()
    transaction(db) {
        SchemaUtils.create(PinnwandGuilds, DiscordMessages, PinboardMessages)
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