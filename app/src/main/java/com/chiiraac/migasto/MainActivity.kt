package com.chiiraac.migasto

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chiiraac.migasto.data.model.ThemeMode
import com.chiiraac.migasto.data.repository.AuthState
import com.chiiraac.migasto.notifications.Notifications
import com.chiiraac.migasto.ui.auth.AuthRoute
import com.chiiraac.migasto.ui.auth.WelcomeRoute
import com.chiiraac.migasto.ui.main.MainRoute
import com.chiiraac.migasto.ui.theme.MiGastoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as MiGastoApplication).container
        if (savedInstanceState == null) handleNotificationIntent(intent)
        splashScreen.setKeepOnScreenCondition {
            container.authRepository.authState.value == AuthState.Loading || container.themeMode.value == null
        }

        setContent {
            val themeMode by container.themeMode.collectAsStateWithLifecycle()
            MiGastoTheme(themeMode ?: ThemeMode.SYSTEM) {
                MiGastoRoot(container)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    /** Al tocar un aviso de movimiento nuevo se abre su grupo. */
    private fun handleNotificationIntent(intent: Intent?) {
        val groupId = intent?.getStringExtra(Notifications.EXTRA_GROUP_ID) ?: return
        (application as MiGastoApplication).container.openGroupRequest.value = groupId
    }
}

@Composable
private fun MiGastoRoot(container: AppContainer) {
    val authState by container.authRepository.authState.collectAsStateWithLifecycle()
    when (val state = authState) {
        AuthState.Loading -> Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
        AuthState.SignedOut -> if (container.isCloud) AuthRoute() else WelcomeRoute()
        is AuthState.SignedIn -> key(state.user.uid) { MainRoute(state.user.uid) }
    }
}
