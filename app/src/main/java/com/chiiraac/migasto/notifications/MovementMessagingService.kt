package com.chiiraac.migasto.notifications

import com.chiiraac.migasto.MiGastoApplication
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch

/** Recibe los avisos de la Cloud Function y los tokens nuevos de Firebase Cloud Messaging. */
class MovementMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val container = (application as MiGastoApplication).container
        container.applicationScope.launch { container.registerPushToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        Notifications.showMovement(
            this,
            notification.title,
            notification.body,
            message.data[Notifications.EXTRA_GROUP_ID],
        )
    }
}
