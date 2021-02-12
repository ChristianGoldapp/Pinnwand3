package command

import RESCAN
import SET_PIN
import SET_PREFIX
import SET_THRESHOLD
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Message
import k
import mention
import stripColons

sealed class Command(val channel: Snowflake, val user: Snowflake, val callback: CommandCallback){
    override fun toString(): String {
        return "Command(channel=$channel, user=$user)"
    }

    abstract fun execute();

    class SetPrefix(channel: Snowflake, user: Snowflake, val prefix: String, callback: CommandCallback) : Command(channel, user, callback) {
        companion object {
            fun parse(message: Message, command: CharSequence, callback: CommandCallback): SetPrefix? {
                val content = command.subSequence(SET_PREFIX.length, command.length).trim()
                val prefix = content.split(" ").firstOrNull() ?: return null
                if(prefix.isBlank() || prefix.length > 32) return null
                return SetPrefix(message.channelId, message.author.k!!.id, prefix, callback)
            }
        }

        override fun toString(): String {
            return "SetPrefix(prefix='$prefix') ${super.toString()}"
        }

        override fun execute() {
            callback.setPrefix(prefix)
            callback.sendMessage(channel){
                setContent("Alright ${user.mention()}, making \"$prefix\" the new command prefix for this server. Note that prefixes may be followed by a space.")
            }
        }

    }

    class SetPinboard(channel: Snowflake, user: Snowflake, callback: CommandCallback) : Command(channel, user, callback) {
        companion object {
            fun parse(message: Message, command: CharSequence, callback: CommandCallback): SetPinboard {
                return SetPinboard(message.channelId, message.author.k!!.id, callback)
            }
        }

        override fun toString(): String {
            return "SetPinboard() ${super.toString()}"
        }

        override fun execute() {
            callback.setPinboard(channel)
            callback.sendMessage(channel){
                setContent("Alright ${user.mention()}, making this channel the pinboard for this server.")
            }
        }

    }

    class SetPin(channel: Snowflake, user: Snowflake, callback: CommandCallback, val emoji: String) : Command(channel, user, callback){
        companion object {
            fun parse(message: Message, command: CharSequence, callback: CommandCallback): SetPin? {
                val content = command.subSequence(SET_PIN.length, command.length).trim()
                val emoji = content.split(" ").firstOrNull()?.stripColons() ?: return null
                if(emoji.isBlank()) return null
                return SetPin(message.channelId, message.author.k!!.id, callback, emoji)
            }
        }

        override fun execute() {
            callback.setPinEmoji(emoji)
            callback.sendMessage(channel){
                setContent("Alright ${user.mention()}, setting the pin emoji for this server to $emoji.")
            }
        }

    }

    class SetThreshold(channel: Snowflake, user: Snowflake, val threshold: Int, callback: CommandCallback) : Command(channel, user, callback) {
        companion object {
            fun parse(message: Message, command: CharSequence, callback: CommandCallback): SetThreshold? {
                val content = command.subSequence(SET_THRESHOLD.length, command.length).trim()
                val threshold = content.split(" ").firstOrNull()?.toIntOrNull() ?: return null
                if(threshold < 1) return null
                return SetThreshold(message.channelId, message.author.k!!.id, threshold, callback)
            }
        }

        override fun toString(): String {
            return "SetThreshold(threshold=$threshold) ${super.toString()}"
        }

        override fun execute() {
            callback.setThreshold(threshold)
            callback.sendMessage(channel){
                setContent("Alright ${user.mention()}, setting the threshold for this server to $threshold.")
            }
        }

    }

    class Leaderboard(channel: Snowflake, user: Snowflake, val page: Int, callback: CommandCallback) : Command(channel, user, callback) {
        companion object {
            fun parse(message: Message, command: CharSequence, callback: CommandCallback): Leaderboard? {
                val content = command.subSequence(RESCAN.length, command.length).trim()
                val page = content.split(" ").firstOrNull()?.toIntOrNull() ?: 1
                if(page < 1) return null
                return Leaderboard(message.channelId, message.author.k!!.id, page, callback)
            }
        }

        override fun toString(): String {
            return "Leaderboard(page=$page) ${super.toString()}"
        }

        override fun execute() {
            callback.leaderboard(channel, page)
        }

    }

    class Nostalgia(channel: Snowflake, user: Snowflake, val query: String, callback: CommandCallback) : Command(channel, user, callback) {
        companion object {
            fun parse(message: Message, command: CharSequence, callback: CommandCallback): Nostalgia? {
                return null
            }
        }

        override fun toString(): String {
            return "Nostalgia(query='$query') ${super.toString()}"
        }

        override fun execute() {
            TODO("Not yet implemented")
        }

    }

    class Ping(channel: Snowflake, user: Snowflake, callback: CommandCallback): Command(channel, user, callback){

        companion object {
            fun parse(message: Message, command: CharSequence, callback: CommandCallback): Ping? {
                return Ping(message.channelId, message.author.k!!.id, callback)
            }
        }

        override fun toString(): String {
            return "Ping(Pong)"
        }

        override fun execute() {
            callback.sendMessage(channel){
                this.setContent("${user.mention()} Pong!")
            }
        }

    }

    class Rescan(channel: Snowflake, user: Snowflake, callback: CommandCallback, val limit: Int): Command(channel, user, callback){

        companion object {
            fun parse(message: Message, command: CharSequence, callback: CommandCallback): Rescan? {
                val content = command.subSequence(RESCAN.length, command.length).trim()
                val limit = content.split(" ").firstOrNull()?.toIntOrNull() ?: return null
                if(limit < 1) return null
                return Rescan(message.channelId, message.author.k!!.id, callback, limit)
            }
        }

        override fun execute() {
            callback.rescan(limit)
        }

    }
}