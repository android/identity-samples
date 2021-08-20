package com.google.android.gms.identity.sample.blockstore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class State(
    val bytes: String? = null
)

val State.areBytesStored get() = bytes != null

/**
 * ViewModel is used to access the Block Store Data and to observe changes to it.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val blockStoreRepository: BlockStoreRepository
) : ViewModel() {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = State(bytes = blockStoreRepository.retrieveBytes())
        }
    }

    fun storeBytes(inputString: String) {
        viewModelScope.launch {
            blockStoreRepository.storeBytes(inputString)
            _state.value = State(bytes = blockStoreRepository.retrieveBytes())
        }
    }

     fun clearBytes() {
        viewModelScope.launch {
            blockStoreRepository.clearBytes()
            _state.value = State(bytes = null)
        }
    }
}