import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Message

data class MessageURL(val guild: Snowflake, val channel: Snowflake, val message: Snowflake){
    constructor(guild: Long, channel: Long, message: Long) : this(Snowflake.of(guild), Snowflake.of(channel), Snowflake.of(message));
    override fun toString(): String {
        return "https://discordapp.com/channels/${guild.asString()}/${channel.asString()}/${message.asString()}"
    }
}