package com.example.mahalleustasi.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.mahalleustasi.MainActivity
import com.example.mahalleustasi.R
import com.example.mahalleustasi.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class MahalleUstasiMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var userRepository: UserRepository
    
    @Inject
    lateinit var auth: FirebaseAuth

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // Cihaza yeni bir FCM token atandığında veya değiştiğinde tetiklenir.
        // Eğer kullanıcı giriş yapmışsa, Firestore'daki fcmToken alanını güncelliyoruz.
        auth.currentUser?.uid?.let { userId ->
            CoroutineScope(Dispatchers.IO).launch {
                userRepository.updateFcmToken(userId, token)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        // Gelen bildirimi yakala ve Android sistem bildirimine çevir
        val title = message.notification?.title ?: message.data["title"] ?: "Yeni Bildirim"
        val body = message.notification?.body ?: message.data["body"] ?: ""

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "mahalle_ustasi_channel_id"
        val channelName = "Mahalle Ustası Bildirimleri"
        val notificationId = Random.nextInt()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0 (Oreo) ve üzeri için NotificationChannel zorunlu
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Uygulama içi güncellemeler ve teklif bildirimleri"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            // İkonu projenin default ikonu (veya ayarlanan bildirim ikonu) olarak değiştirilebilir
            // Şimdilik launcher ikonu kullanalım
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
