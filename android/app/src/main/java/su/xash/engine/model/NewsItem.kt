package su.xash.engine.model

import org.json.JSONArray
import org.json.JSONObject

data class NewsItem(
	val id: String,
	val author: String,
	val content: String,
	val timestamp: String,
	val images: List<String>,
	val jumpUrl: String?,
) {
	companion object {
		fun fromJson(obj: JSONObject): NewsItem {
			val imagesArray = obj.optJSONArray("images")
			val images = mutableListOf<String>()
			if (imagesArray != null) {
				for (i in 0 until imagesArray.length()) {
					images.add(imagesArray.getString(i))
				}
			}

			return NewsItem(
				id = obj.optString("id"),
				author = obj.optString("author", "?"),
				content = obj.optString("content", ""),
				timestamp = obj.optString("timestamp", ""),
				images = images,
				jumpUrl = obj.optString("jump_url").ifBlank { null },
			)
		}

		fun listFromJson(text: String): List<NewsItem> {
			val array = JSONArray(text)
			val items = mutableListOf<NewsItem>()
			for (i in 0 until array.length()) {
				items.add(fromJson(array.getJSONObject(i)))
			}
			return items
		}
	}
}
