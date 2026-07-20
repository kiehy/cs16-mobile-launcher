package su.xash.engine.ui.news

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import su.xash.engine.R
import su.xash.engine.adapters.NewsAdapter
import su.xash.engine.databinding.FragmentNewsBinding

class NewsFragment : Fragment() {
	private var _binding: FragmentNewsBinding? = null
		private val binding get() = _binding!!

		private val newsViewModel: NewsViewModel by viewModels()

			private lateinit var newsAdapter: NewsAdapter

				override fun onCreateView(
					inflater: LayoutInflater,
					container: ViewGroup?,
					savedInstanceState: Bundle?,
				): View {
					_binding = FragmentNewsBinding.inflate(inflater, container, false)

					newsAdapter = NewsAdapter { item ->
						openDiscordUrl(item.jumpUrl)
					}

					binding.newsList.apply {
						layoutManager = LinearLayoutManager(requireContext())
						adapter = newsAdapter
						setHasFixedSize(false)
					}

					return binding.root
				}

				override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
					super.onViewCreated(view, savedInstanceState)

					binding.swipeRefresh.setOnRefreshListener {
						newsViewModel.refresh()
					}

					newsViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
						binding.swipeRefresh.isRefreshing = isLoading
					}

					newsViewModel.newsItems.observe(viewLifecycleOwner) { items ->
						newsAdapter.submitList(items)

						binding.emptyText.visibility =
						if (items.isEmpty()) View.VISIBLE else View.GONE
					}

					newsViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
						if (message != null) {
							Toast.makeText(
								requireContext(),
										   getString(R.string.news_load_error),
										   Toast.LENGTH_SHORT,
							).show()
						}
					}

					newsViewModel.refresh()
				}

				private fun openDiscordUrl(url: String?) {
					if (url.isNullOrBlank()) {
						return
					}

					val uri = try {
						Uri.parse(url)
					} catch (_: Exception) {
						showOpenLinkError()
						return
					}

					val intent = Intent(Intent.ACTION_VIEW, uri)

					try {
						startActivity(intent)
					} catch (_: ActivityNotFoundException) {
						showOpenLinkError()
					} catch (_: SecurityException) {
						showOpenLinkError()
					}
				}

				private fun showOpenLinkError() {
					Toast.makeText(
						requireContext(),
								   getString(R.string.news_open_link_error),
								   Toast.LENGTH_SHORT,
					).show()
				}

				override fun onDestroyView() {
					binding.newsList.adapter = null
					_binding = null
					super.onDestroyView()
				}
}
