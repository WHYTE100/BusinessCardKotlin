package io.pridetechnologies.businesscard.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.pridetechnologies.businesscard.Constants
import io.pridetechnologies.businesscard.HomeActivity
import io.pridetechnologies.businesscard.R
import io.pridetechnologies.businesscard.UserRequestDetailsActivity
import io.pridetechnologies.businesscard.activities.NewCardActivity
import kotlin.random.Random

private const val CHANNEL_ID = "channel"

class FirebaseMessagingService : FirebaseMessagingService() {

    val constants = Constants()
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val map = mapOf(
            "token" to token
        )
        constants.db.collection("users").document(constants.currentUserId.toString()).set(map, SetOptions.merge())
    }
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val nTitle = message.data["title"]
        val nBody = message.data["message"]
        val uid = message.data["uid"]
        val type = message.data["type"]

        when (type) {
            "declined_user_request" -> {
                val intent = Intent(this, NewCardActivity::class.java)
                intent.putExtra("user_id", uid)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notificationId = Random.nextInt()

                createNotificationChannel(notificationManager)

                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                val pendingIntent = PendingIntent.getActivity(this, 0, intent, FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(nTitle)
                    .setContentText(nBody)
                    .setSmallIcon(R.drawable.card_round)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent)
                    .build()
                notificationManager.notify(notificationId, notification)
            }
            "user_request" -> {
                val intent = Intent(this, UserRequestDetailsActivity::class.java)
                intent.putExtra("user_id", uid)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notificationId = Random.nextInt()

                createNotificationChannel(notificationManager)

                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                val pendingIntent = PendingIntent.getActivity(this, 0, intent, FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(nTitle)
                    .setContentText(nBody)
                    .setSmallIcon(R.drawable.card_round)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent)
                    .build()
                notificationManager.notify(notificationId, notification)
            }else -> {
            val intent = Intent(this, HomeActivity::class.java)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = Random.nextInt()

            createNotificationChannel(notificationManager)

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(nTitle)
                .setContentText(nBody)
                .setSmallIcon(R.drawable.card_round)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .build()
            notificationManager.notify(notificationId, notification)
            }
        }
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channelName = "channelName"
        val channel = NotificationChannel(CHANNEL_ID, channelName, IMPORTANCE_HIGH).apply {
            description = "Notification Channel"
        }
        notificationManager.createNotificationChannel(channel)
    }
}