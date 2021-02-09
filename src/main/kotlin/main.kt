import discord4j.core.DiscordClientBuilder
import discord4j.gateway.retry.RetryOptions
import reactor.core.scheduler.Schedulers
import java.io.File
import java.time.Duration

fun main(args: Array<String>) {
    val token = File(args[0]).readText()
    val discord = DiscordClientBuilder(token).run {
        retryOptions = RetryOptions(Duration.ofSeconds(10), Duration.ofMinutes(30), 8, Schedulers.elastic())
        build()
    }
    discord.login().block()
}