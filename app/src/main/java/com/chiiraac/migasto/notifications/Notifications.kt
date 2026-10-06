package com.chiiraac.migasto.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.chiiraac.migasto.MainActivity
import com.chiiraac.migasto.R
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/** Canal y avisos de "movimiento nuevo en el grupo" (modo nube). */
object Notifications {
    const val CHANNEL_MOVEMENTS = "movements"

    /** Avisos generales enviados desde la consola de Firebase (Messaging) al tema [TOPIC_NEWS]. */
    const val CHANNEL_NEWS = "news"
    const val TOPIC_NEWS = "novedades"

    /** Extra con el grupo a abrir al tocar un aviso (la Cloud Function lo manda con el mismo nombre). */
    const val EXTRA_GROUP_ID = "groupId"

    fun createChannels(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_MOVEMENTS,
            context.getString(R.string.notification_channel_movements),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.notification_channel_movements_desc) }
        val news = NotificationChannel(
            CHANNEL_NEWS,
            context.getString(R.string.notification_channel_news),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.notification_channel_news_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(channel, news))
    }

    /** El sistema deja mostrar avisos (permiso concedido en Android 13+ y no desactivados). */
    fun enabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
                )

    /** Muestra un aviso recibido con la app abierta (con la app cerrada lo muestra el sistema). */
    fun show(context: Context, channelId: String, title: String?, body: String?, groupId: String?) {
        if (!enabled(context)) return
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_GROUP_ID, groupId)
        val pending = PendingIntent.getActivity(
            context,
            groupId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_migasto)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        } catch (_: SecurityException) {
            // Permiso retirado justo ahora: no se muestra.
        }
    }
}

/** Avisos de este móvil: su token y la suscripción a las novedades (sustituible en los tests). */
interface PushTokens {
    suspend fun current(): String?

    /** Se apunta o se da de baja de los avisos generales de MiGasto. */
    suspend fun setNewsSubscribed(subscribed: Boolean)
}

class FirebasePushTokens : PushTokens {
    override suspend fun current(): String? =
        runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()

    override suspend fun setNewsSubscribed(subscribed: Boolean) {
        val messaging = FirebaseMessaging.getInstance()
        runCatching {
            if (subscribed) {
                messaging.subscribeToTopic(Notifications.TOPIC_NEWS).await()
            } else {
                messaging.unsubscribeFromTopic(Notifications.TOPIC_NEWS).await()
            }
        }
    }
}
