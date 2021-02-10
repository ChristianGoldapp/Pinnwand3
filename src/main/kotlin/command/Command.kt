package command

import SET_PREFIX
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Message
import k
import mention

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
        }

    }

    class SetPinboard(channel: Snowflake, user: Snowflake, callback: CommandCallback) : Command(channel, user, callback) {
        companion object {
            fun parse(message: Message, command: CharSequence, callback: CommandCallback): SetPinboard? {
                return null
            }
        }

        override fun toString(): String {
            return "SetPinboard() ${super.toString()}"
        }

        override fun execute() {
            TODO("Not yet implemented")
        }

    }

    class SetThreshold(channel: Snowflake, user: Snowflake, val threshold: Int, callback: CommandCallback) : Command(channel, user, callback) {
        companion object {
            fun parse(message: Message, command: CharSequence, callback: CommandCallback): SetThreshold? {
                return null
            }
        }

        override fun toString(): String {
            return "SetThreshold(threshold=$threshold) ${super.toString()}"
        }

        override fun execute() {
            TODO("Not yet implemented")
        }

    }

    class Leaderboard(channel: Snowflake, user: Snowflake, val page: Int, callback: CommandCallback) : Command(channel, user, callback) {
        companion object {
            fun parse(message: Message, command: CharSequence, callback: CommandCallback): Leaderboard? {
                return null
            }
        }

        override fun toString(): String {
            return "Leaderboard(page=$page) ${super.toString()}"
        }

        override fun execute() {
            TODO("Not yet implemented")
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
}