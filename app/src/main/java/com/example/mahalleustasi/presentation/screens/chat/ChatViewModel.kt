package com.example.mahalleustasi.presentation.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.ChatMessage
import com.example.mahalleustasi.domain.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val chatId: String = savedStateHandle.get<String>("chatId") ?: ""
    val currentUserId: String get() = auth.currentUser?.uid ?: ""
    val currentUserName: String get() =
        auth.currentUser?.displayName ?: auth.currentUser?.email?.substringBefore("@") ?: "Ben"

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        if (chatId.isNotBlank()) {
            loadMessages()
        }
    }

    private fun loadMessages() {
        chatRepository.getMessages(chatId).onEach { result ->
            when (result) {
                is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, messages = result.data ?: emptyList())
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                else -> Unit
            }
        }.launchIn(viewModelScope)
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || chatId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }

            val message = ChatMessage(
                chatId = chatId,
                senderId = currentUserId,
                senderName = currentUserName,
                text = text.trim(),
                createdAt = System.currentTimeMillis()
            )

            val result = chatRepository.sendMessage(message)
            if (result is Resource.Error) {
                _uiState.update { it.copy(isSending = false, error = result.message) }
            } else {
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }
}
