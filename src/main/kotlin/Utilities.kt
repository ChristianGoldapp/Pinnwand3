import java.util.*

val <T> Optional<T>.k get() = if (this.isPresent) this.get() else null

val <T> T?.o get() = Optional.ofNullable(this)