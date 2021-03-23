import db.DBConfig
import db.DiscordMessages
import db.PinboardMessages
import db.PinnwandGuilds
import discord4j.core.DiscordClient
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.system.exitProcess

object Main {

    val log = LoggerFactory.getLogger(this.javaClass)

    fun main(args: Array<String>) {
        val db = DBConfig.init(File(args[1])).connect()
        transaction(db) {
            SchemaUtils.create(PinnwandGuilds, DiscordMessages, PinboardMessages)
        }
        val token = File(args[0]).readText()
        val client = DiscordClient.create(token).login().doOnError {
            log.error("Could not connect Discord client")
            exitProcess(-1)
        }.block()!!
        val guildInit = GuildInitialization(client)

        guildInit.subscribe()

        client.onDisconnect().block()
    }
}