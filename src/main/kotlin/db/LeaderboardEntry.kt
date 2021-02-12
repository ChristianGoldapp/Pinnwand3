package db

import discord4j.common.util.Snowflake
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction

data class LeaderboardEntry(val author: Snowflake, val totalPins: Int)

object Leaderboard {
    fun tally(guild: Snowflake) = transaction {
        val guildId = guild.asLong()
        val results = DiscordMessages
            .slice(DiscordMessages.author, DiscordMessages.pinCount.sum(), DiscordMessages.guild)
            .select {
                DiscordMessages.guild eq guildId
            }.groupBy(DiscordMessages.author)
            .execute(this)
        val list = ArrayList<LeaderboardEntry>()
        results?.let {
            while (it.next()) {
                //Remember: cursor fields are 1-indexed
                val author = it.getLong(1)
                val tally = it.getInt(2)
                list.add(LeaderboardEntry(Snowflake.of(author), tally))
            }
        }
        //Maybe do this as an SQL order-by
        list.sortByDescending { it.totalPins }
        list
    }
}