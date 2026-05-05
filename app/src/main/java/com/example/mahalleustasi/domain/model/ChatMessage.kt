package com.example.mahalleustasi.domain.model

data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Chat(
    val id: String = "",          // "${jobId}_${offerId}"
    val jobId: String = "",
    val jobTitle: String = "",
    val offerId: String = "",
    val buyerId: String = "",     // İlanı veren (hizmet arayan)
    val buyerName: String = "",
    val sellerId: String = "",    // Teklifi veren (hizmet sağlayan)
    val sellerName: String = "",
    val acceptedAt: Long = System.currentTimeMillis()
)
