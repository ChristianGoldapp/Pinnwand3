package db

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.jetbrains.exposed.sql.Database
import java.io.File

@Serializable
data class DBConfig(val uri: String, val driver: String, val creds: DBCredentials) {
    companion object {
        fun init(file: File): DBConfig {
            return Yaml.default.decodeFromString<DBConfig>(file.readText())
        }
    }

    fun connect(): Database {
        return Database.connect(uri, driver, creds.user, creds.password)
    }
}

@Serializable
data class DBCredentials(val user: String, val password: String)