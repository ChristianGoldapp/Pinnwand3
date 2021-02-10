package command

interface CommandCallback {

    fun setPrefix(newPrefix: String)

    fun getPrefix(): String

}