package su.xash.engine.model

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL

object ServerRepository {

	private const val SERVERS_URL =
	"https://kiehy.github.io/xash-news-data/servers.json"

	private const val PREFERENCES_NAME =
	"server_repository"

	private const val CACHE_KEY =
	"cached_servers_json"

	private const val CONNECTION_TIMEOUT =
	15_000

	private const val READ_TIMEOUT =
	15_000

	/**
	 * Tenta baixar a lista atual do GitHub.
	 *
	 * Quando o download funciona:
	 * - valida o JSON;
	 * - salva uma cópia local;
	 * - devolve a lista atualizada.
	 *
	 * Quando falha:
	 * - tenta devolver a última lista salva em cache.
	 */
	fun loadServers(context: Context): ServerLoadResult {
		return try {
			val json = downloadServersJson()
			val servers = parseServers(json)

			saveCache(context, json)

			ServerLoadResult(
				servers = servers,
				loadedFromCache = false,
				errorMessage = null,
			)
		} catch (networkException: Exception) {
			loadFromCache(
				context = context,
				 originalException = networkException,
			)
		}
	}

	private fun downloadServersJson(): String {
		var connection: HttpURLConnection? = null

		try {
			connection = URL(SERVERS_URL)
			.openConnection() as HttpURLConnection

			connection.requestMethod = "GET"
			connection.connectTimeout = CONNECTION_TIMEOUT
			connection.readTimeout = READ_TIMEOUT
			connection.instanceFollowRedirects = true
			connection.useCaches = false

			connection.setRequestProperty(
				"Accept",
				"application/json",
			)

			connection.setRequestProperty(
				"User-Agent",
				"CS16-Client-Android",
			)

			connection.connect()

			val responseCode = connection.responseCode

			if (responseCode !in 200..299) {
				throw IllegalStateException(
					"O GitHub respondeu com o código HTTP $responseCode."
				)
			}

			val contentType = connection.contentType.orEmpty()

			if (
				contentType.contains("text/html", ignoreCase = true)
			) {
				throw IllegalStateException(
					"O endereço retornou uma página HTML em vez do JSON."
				)
			}

			return BufferedInputStream(connection.inputStream)
			.bufferedReader(Charsets.UTF_8)
			.use { reader ->
				reader.readText()
			}
			.trim()
		} finally {
			connection?.disconnect()
		}
	}

	private fun parseServers(json: String): List<ServerEntry> {
		if (json.isBlank()) {
			throw JSONException(
				"O arquivo servers.json está vazio."
			)
		}

		val root = JSONObject(json)

		if (!root.has("servers")) {
			throw JSONException(
				"O arquivo JSON não possui a propriedade \"servers\"."
			)
		}

		val serversArray = root.getJSONArray("servers")
		val servers = mutableListOf<ServerEntry>()

		for (index in 0 until serversArray.length()) {
			val serverObject = serversArray.getJSONObject(index)

			val enabled = serverObject.optBoolean(
				"enabled",
				true,
			)

			if (!enabled) {
				continue
			}

			val id = serverObject
			.optString("id")
			.trim()

			val name = serverObject
			.optString("name")
			.trim()

			val address = serverObject
			.optString("address")
			.trim()

			val port = serverObject.optInt(
				"port",
				27015,
			)

			val description = serverObject
			.optString("description")
			.trim()

			val game = serverObject
			.optString(
				"game",
			  "cstrike",
			)
			.trim()

			if (name.isBlank()) {
				continue
			}

			if (address.isBlank()) {
				continue
			}

			if (port !in 1..65535) {
				continue
			}

			servers += ServerEntry(
				id = id.ifBlank {
					"$address:$port"
				},
				name = name,
				ip = address,
				port = port,
				description = description,
				game = game.ifBlank {
					"cstrike"
				},
				enabled = true,
			)
		}

		if (servers.isEmpty()) {
			throw JSONException(
				"Nenhum servidor ativo e válido foi encontrado."
			)
		}

		return servers
	}

	private fun saveCache(
		context: Context,
		json: String,
	) {
		context
		.getSharedPreferences(
			PREFERENCES_NAME,
			Context.MODE_PRIVATE,
		)
		.edit()
		.putString(CACHE_KEY, json)
		.apply()
	}

	private fun loadFromCache(
		context: Context,
		originalException: Exception,
	): ServerLoadResult {
		val cachedJson = context
		.getSharedPreferences(
			PREFERENCES_NAME,
			Context.MODE_PRIVATE,
		)
		.getString(CACHE_KEY, null)

		if (cachedJson.isNullOrBlank()) {
			return ServerLoadResult(
				servers = emptyList(),
									loadedFromCache = false,
						   errorMessage =
						   originalException.message
						   ?: "Não foi possível carregar os servidores.",
			)
		}

		return try {
			ServerLoadResult(
				servers = parseServers(cachedJson),
							 loadedFromCache = true,
					errorMessage =
					originalException.message,
			)
		} catch (_: Exception) {
			ServerLoadResult(
				servers = emptyList(),
							 loadedFromCache = false,
					errorMessage =
					originalException.message
					?: "Não foi possível carregar os servidores.",
			)
		}
	}
}

data class ServerLoadResult(
	val servers: List<ServerEntry>,
	val loadedFromCache: Boolean,
	val errorMessage: String?,
)
