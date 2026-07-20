package su.xash.engine.ui.news

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import su.xash.engine.model.NewsItem
import su.xash.engine.model.NewsRepository

class NewsViewModel(application: Application) : AndroidViewModel(application) {
	private val repository = NewsRepository(application)

	val newsItems: LiveData<List<NewsItem>> get() = _newsItems
	private val _newsItems = MutableLiveData<List<NewsItem>>(emptyList())

	val isLoading: LiveData<Boolean> get() = _isLoading
	private val _isLoading = MutableLiveData(false)

	val errorMessage: LiveData<String?> get() = _errorMessage
	private val _errorMessage = MutableLiveData<String?>(null)

	fun refresh() {
		if (isLoading.value == true)
			return

		_isLoading.value = true
		viewModelScope.launch {
			when (val result = repository.fetchNews()) {
				is NewsRepository.Result.Success -> {
					_newsItems.postValue(result.items)
					_errorMessage.postValue(null)
				}
				is NewsRepository.Result.Error -> {
					_errorMessage.postValue(result.cause.message)
				}
				NewsRepository.Result.NotConfigured -> {
					_errorMessage.postValue(null)
				}
			}
			_isLoading.postValue(false)
		}
	}
}
