import db.PinnwandGuilds
import discord4j.core.DiscordClientBuilder
import discord4j.gateway.retry.RetryOptions
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import reactor.core.scheduler.Schedulers
import java.io.File
import java.time.Duration

fun main(args: Array<String>) {
    val db = Database.connect("jdbc:sqlite:test.sqlite")
    transaction(db) {
        SchemaUtils.create(PinnwandGuilds)
    }
    val token = File(args[0]).readText()
    val discord = DiscordClientBuilder(token).run {
        retryOptions = RetryOptions(Duration.ofSeconds(10), Duration.ofMinutes(30), 8, Schedulers.elastic())
        build()
    }
    val guildInit = GuildInitialization(discord)

    guildInit.subscribe()

    discord.login().block()
}