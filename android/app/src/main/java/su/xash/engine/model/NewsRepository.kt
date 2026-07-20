package su.xash.engine.model

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class NewsRepository(private val ctx: Context) {
	private val appPreferences: SharedPreferences =
		ctx.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

	sealed class Result {
		data class Success(val items: List<NewsItem>) : Result()
		data class Error(val cause: Throwable) : Result()
		object NotConfigured : Result()
	}

	suspend fun fetchNews(): Result = withContext(Dispatchers.IO) {
		val url = "https://kiehy.github.io/xash-news-data/news.json"

		var connection: HttpURLConnection? = null
		try {
			connection = URL(url).openConnection() as HttpURLConnection
			connection.setRequestProperty("Cache-Control", "no-cache")
			connection.setRequestProperty("Pragma", "no-cache")
			connection.instanceFollowRedirects = true
			connection.connectTimeout = 10_000
			connection.readTimeout = 10_000
			connection.requestMethod = "GET"

			if (connection.responseCode != HttpURLConnection.HTTP_OK) {
				return@withContext Result.Error(
					IllegalStateException("HTTP ${connection.responseCode}")
				)
			}

			val text = connection.inputStream.bufferedReader().use { it.readText() }
			Result.Success(NewsItem.listFromJson(text))
		} catch (e: Exception) {
			Result.Error(e)
		} finally {
			connection?.disconnect()
		}
	}
}
