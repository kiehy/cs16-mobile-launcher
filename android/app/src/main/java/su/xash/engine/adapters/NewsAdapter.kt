package su.xash.engine.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import su.xash.engine.R
import su.xash.engine.databinding.CardNewsBinding
import su.xash.engine.model.NewsItem

class NewsAdapter(
	private val onOpenInDiscord: (NewsItem) -> Unit,
) : ListAdapter<NewsItem, NewsAdapter.NewsViewHolder>(DiffCallback()) {

	override fun onCreateViewHolder(
		parent: ViewGroup,
		viewType: Int,
	): NewsViewHolder {
		val binding = CardNewsBinding.inflate(
			LayoutInflater.from(parent.context),
											  parent,
										false,
		)

		return NewsViewHolder(binding)
	}

	override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
		holder.bind(getItem(position))
	}

	override fun onViewRecycled(holder: NewsViewHolder) {
		super.onViewRecycled(holder)
		holder.recycle()
	}

	private class DiffCallback : DiffUtil.ItemCallback<NewsItem>() {
		override fun areItemsTheSame(
			oldItem: NewsItem,
			newItem: NewsItem,
		): Boolean {
			return oldItem.id == newItem.id
		}

		override fun areContentsTheSame(
			oldItem: NewsItem,
			newItem: NewsItem,
		): Boolean {
			return oldItem == newItem
		}
	}

	inner class NewsViewHolder(
		private val binding: CardNewsBinding,
	) : RecyclerView.ViewHolder(binding.root) {

		fun bind(item: NewsItem) {
			binding.newsAuthor.text = item.author
			binding.newsTimestamp.text = item.timestamp
			binding.newsContent.text = item.content

			bindImage(item)
			bindDiscordButton(item)
		}

		private fun bindImage(item: NewsItem) {
			val imageUrl = item.images.firstOrNull()
			?.trim()
			?.takeIf { it.isNotEmpty() }

			if (imageUrl == null) {
				binding.newsImage.visibility = View.GONE
				binding.newsImage.setImageDrawable(null)
				return
			}

			binding.newsImage.visibility = View.VISIBLE

			binding.newsImage.load(imageUrl) {
				crossfade(true)
				placeholder(R.drawable.news_image_placeholder)
				error(R.drawable.news_image_placeholder)
			}
		}

		private fun bindDiscordButton(item: NewsItem) {
			val jumpUrl = item.jumpUrl

			if (jumpUrl.isNullOrBlank()) {
				binding.newsOpenDiscord.visibility = View.GONE
				binding.newsOpenDiscord.setOnClickListener(null)
				return
			}

			binding.newsOpenDiscord.visibility = View.VISIBLE
			binding.newsOpenDiscord.setOnClickListener {
				onOpenInDiscord(item)
			}
		}

		fun recycle() {
			binding.newsImage.setImageDrawable(null)
			binding.newsOpenDiscord.setOnClickListener(null)
		}
	}
}
