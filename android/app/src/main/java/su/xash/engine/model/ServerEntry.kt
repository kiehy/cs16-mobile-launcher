package su.xash.engine.model

data class ServerEntry(
	val id: String,
	val name: String,
	val ip: String,
	val port: Int = 27015,
	val description: String = "",
	val game: String = "cstrike",
	val enabled: Boolean = true,
	val status: ServerStatus = ServerStatus.Loading,
) {
	val connectTarget: String
	get() = "$ip:$port"
}

sealed class ServerStatus {

	data object Loading : ServerStatus()

	data class Online(
		val serverName: String,
		val map: String,
		val players: Int,
		val maxPlayers: Int,
		val bots: Int,
		val ping: Long,
	) : ServerStatus()

	data class Offline(
		val reason: String = "Sem resposta",
	) : ServerStatus()
}
