import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.GuildMessageChannel

class Pinboard(initialThreshold: Int) {

    var channel: GuildMessageChannel? = null
    var threshold: Int = initialThreshold

    fun updateBasedOn(original: Message, pinCount: Int){
        if(pinCount >= threshold){
            shouldPin(original, pinCount)
        } else shouldUnpin(original, pinCount)
    }

    fun shouldPin(original: Message, pinCount: Int){
        println("Should pin ($pinCount pins): $original")
    }

    fun shouldUnpin(original: Message, pinCount: Int){
        println("Should unpin ($pinCount pins): $original")
    }
}