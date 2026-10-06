package com.chiiraac.migasto

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.chiiraac.migasto.data.local.LocalAuthRepository
import com.chiiraac.migasto.data.local.LocalDatabase
import com.chiiraac.migasto.data.local.LocalFinanceRepository
import com.chiiraac.migasto.data.local.LocalPhotoStore
import com.chiiraac.migasto.data.prefs.UserPreferences
import com.chiiraac.migasto.data.remote.FirebaseAuthRepository
import com.chiiraac.migasto.data.remote.FirestoreFinanceRepository
import com.chiiraac.migasto.data.repository.AuthRepository
import com.chiiraac.migasto.data.repository.FinanceRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import com.chiiraac.migasto.data.model.ThemeMode

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * Dependencias de la app. Si el proyecto incluye `google-services.json`, Firebase se
 * inicializa solo y la app funciona en modo nube (grupos compartidos entre usuarios).
 * Si no, todo se guarda en el dispositivo (modo local).
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val isCloud: Boolean = System.getProperty("migasto.forceLocal") != "true" &&
        FirebaseApp.getApps(appContext).isNotEmpty()

    val preferences = UserPreferences(appContext.settingsDataStore)

    /** Tema elegido; null mientras se lee (la pantalla de inicio espera a tenerlo). */
    val themeMode: StateFlow<ThemeMode?> = preferences.themeMode
        .stateIn(applicationScope, SharingStarted.Eagerly, null)

    val authRepository: AuthRepository by lazy {
        if (isCloud) {
            FirebaseAuthRepository(FirebaseAuth.getInstance())
        } else {
            LocalAuthRepository(preferences, applicationScope)
        }
    }

    val financeRepository: FinanceRepository by lazy {
        if (isCloud) {
            FirestoreFinanceRepository(FirebaseFirestore.getInstance())
        } else {
            LocalFinanceRepository(
                database = LocalDatabase.create(appContext),
                photos = LocalPhotoStore(File(appContext.filesDir, "photos")),
            )
        }
    }
}
