package com.app.kokodak

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CrawlerViewModel : ViewModel() {

    // MutableLiveData за вътрешна промяна
    private val _searchResults = MutableLiveData<List<SearchResult>>()
    // Публична LiveData за четене в MainActivity
    val searchResults: LiveData<List<SearchResult>> = _searchResults

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Основен метод за търсене
     */
    fun search(query: String) {
        if (query.isBlank()) {
            _error.value = "Моля, въведете дума за търсене"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Изпълняваме мрежовата заявка или кроулинга в IO нишка
                val results = withContext(Dispatchers.IO) {
                    performSearch(query)
                }

                _searchResults.value = results
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Възникна неочаквана грешка"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Заместете логиката тук с вашия истински Ktor/Retrofit call или HTML crawler (JSoup)
     */
    private fun performSearch(query: String): List<SearchResult> {
        // Примерни данни (Mock data) за тест:
        return listOf(
            SearchResult(
                title = "Резултат за: $query",
                url = "https://example.com/search?q=$query",
                snippet = "Това е примерен текст от намерената страница за вашата заявка '$query'."
            ),
            SearchResult(
                title = "Kokodak Search - $query",
                url = "https://kokodak.app/info",
                snippet = "Допълнително описание за $query в приложението."
            )
        )
    }
}