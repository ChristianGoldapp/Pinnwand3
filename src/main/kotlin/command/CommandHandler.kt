import command.Command
import command.CommandCallback
import discord4j.core.`object`.entity.Message

class CommandHandler(val callback: CommandCallback) {
    fun parseCommand(message: Message): Command? {
        try {
            val prefix = callback.getPrefix()
            val text = message.content
            if (!text.startsWith(prefix)) return null
            if (!message.author.isPresent) return null
            //Skip prefix
            val command = text.subSequence(prefix.length, text.length).trim()
            return if (command.startsWith(SET_PREFIX, true)) {
                Command.SetPrefix.parse(message, command, callback)
            } else if (command.startsWith(SET_PINBOARD, true)) {
                Command.SetPinboard.parse(message, command, callback)
            } else if (command.startsWith(SET_THRESHOLD, true)) {
                Command.SetThreshold.parse(message, command, callback)
            } else if (command.startsWith(LEADERBOARD, true)) {
                Command.Leaderboard.parse(message, command, callback)
            } else if (command.startsWith(NOSTALGIA, true)) {
                Command.Nostalgia.parse(message, command, callback)
            } else if (command.startsWith(PING, true)) {
                Command.Ping.parse(message, command, callback)
            } else if (command.startsWith(SET_PIN, true)) {
                Command.SetPin.parse(message, command, callback)
            } else if (command.startsWith(RESCAN, true)) {
                Command.Rescan.parse(message, command, callback)
            } else {
                null
            }
        } catch (ex: Exception){
            return null
        }
    }

    fun onMessage(message: Message) {
        val command = parseCommand(message) ?: return
        println("Executing: $command")
        command.execute()
    }
}

const val SET_PREFIX = "prefix"
const val SET_PINBOARD = "pinboard"
const val SET_PIN = "setpin"
const val SET_THRESHOLD = "threshold"
const val LEADERBOARD = "leaderboard"
const val NOSTALGIA = "nostalgia"
const val PING = "ping"
const val RESCAN = "rescan"