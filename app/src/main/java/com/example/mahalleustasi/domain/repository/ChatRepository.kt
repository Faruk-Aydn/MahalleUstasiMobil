package com.example.mahalleustasi.domain.repository

import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Chat
import com.example.mahalleustasi.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    /**
     * Teklif kabul edildiğinde çağrılır.
     * Eğer chat dökümanı yoksa oluşturur, varsa getirir.
     */
    suspend fun getOrCreateChat(chat: Chat): Resource<Chat>

    /** Gerçek zamanlı mesaj listesi (Snapshot Listener) */
    fun getMessages(chatId: String): Flow<Resource<List<ChatMessage>>>

    /** Yeni mesaj gönder */
    suspend fun sendMessage(message: ChatMessage): Resource<Unit>

    /** Kullanıcının dahil olduğu chat listesi */
    fun getChatsForUser(userId: String): Flow<Resource<List<Chat>>>
}
