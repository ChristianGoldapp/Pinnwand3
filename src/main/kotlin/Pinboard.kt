import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.GuildMessageChannel

class Pinboard(initialThreshold: Int, initialChannel: GuildMessageChannel?) {

    var channel: GuildMessageChannel? = initialChannel
    var threshold: Int = initialThreshold

    fun updateBasedOn(original: Message, pinCount: Int){
        if(pinCount >= threshold){
            shouldPin(original, pinCount)
        } else shouldUnpin(original.id, pinCount)
    }

    fun shouldPin(original: Message, pinCount: Int){
        println("Should pin ($pinCount pins): $original")
    }

    fun shouldUnpin(originalId: Snowflake, pinCount: Int){
        println("Should unpin ($pinCount pins): $originalId")
    }
}