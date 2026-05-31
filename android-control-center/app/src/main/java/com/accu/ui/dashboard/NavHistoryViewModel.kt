package com.accu.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accu.data.repositories.NavigationHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavHistoryViewModel @Inject constructor(
    private val repo: NavigationHistoryRepository,
) : ViewModel() {

    fun record(route: String) {
        viewModelScope.launch { repo.record(route) }
    }
}
