package com.example.mahalleustasi.data.repository

import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.Chat
import com.example.mahalleustasi.domain.model.ChatMessage
import com.example.mahalleustasi.domain.repository.ChatRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatRepository {

    private val chatsCollection = firestore.collection("chats")

    override suspend fun getOrCreateChat(chat: Chat): Resource<Chat> {
        return try {
            val docRef = chatsCollection.document(chat.id)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                // Chat zaten var — mevcut dökümanı döndür
                val existingChat = snapshot.toObject(Chat::class.java)
                Resource.Success(existingChat ?: chat)
            } else {
                // Yeni chat oluştur
                docRef.set(chat).await()
                Resource.Success(chat)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Sohbet oluşturulamadı.")
        }
    }

    override fun getMessages(chatId: String): Flow<Resource<List<ChatMessage>>> = callbackFlow {
        trySend(Resource.Loading)

        val listener = chatsCollection.document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Mesajlar alınamadı"))
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull {
                    it.toObject(ChatMessage::class.java)
                } ?: emptyList()
                trySend(Resource.Success(messages))
            }

        awaitClose { listener.remove() }
    }

    override suspend fun sendMessage(message: ChatMessage): Resource<Unit> {
        return try {
            val docRef = chatsCollection.document(message.chatId)
                .collection("messages")
                .document()

            docRef.set(message.copy(id = docRef.id)).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Mesaj gönderilemedi.")
        }
    }

    override fun getChatsForUser(userId: String): Flow<Resource<List<Chat>>> = callbackFlow {
        trySend(Resource.Loading)

        // Hem alıcı hem satıcı olarak dahil olduğu chatler
        // Firestore'da OR sorgusu için iki ayrı sorgu yapıyoruz
        val asBuyer = chatsCollection
            .whereEqualTo("buyerId", userId)
            .addSnapshotListener { snapshot, _ ->
                val chats = snapshot?.documents?.mapNotNull { it.toObject(Chat::class.java) } ?: emptyList()
                trySend(Resource.Success(chats))
            }

        awaitClose { asBuyer.remove() }
    }
}
