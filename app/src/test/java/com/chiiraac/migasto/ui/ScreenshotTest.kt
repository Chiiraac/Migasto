package com.chiiraac.migasto.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chiiraac.migasto.TestData
import com.chiiraac.migasto.data.model.ThemeMode
import com.chiiraac.migasto.ui.auth.AuthScreen
import com.chiiraac.migasto.ui.auth.AuthUiState
import com.chiiraac.migasto.ui.auth.WelcomeScreen
import com.chiiraac.migasto.ui.components.LocalToday
import com.chiiraac.migasto.ui.groups.GroupSettingsDialog
import com.chiiraac.migasto.ui.groups.RemoveMemberDialog
import com.chiiraac.migasto.ui.main.MainActions
import com.chiiraac.migasto.ui.main.MainScreen
import com.chiiraac.migasto.ui.main.MainTab
import com.chiiraac.migasto.ui.main.MainUiState
import com.chiiraac.migasto.ui.movement.MovementEditorContent
import com.chiiraac.migasto.ui.stats.StatsPeriod
import com.chiiraac.migasto.ui.stats.StatsScreen
import com.chiiraac.migasto.ui.theme.MiGastoTheme
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import java.util.Locale
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renderiza las pantallas principales a PNG (build/screenshots) para revisarlas
 * sin emulador y para las capturas de Google Play.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es-rES-w411dp-h891dp-xxhdpi")
@OptIn(ExperimentalRoborazziApi::class)
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setLocale() {
        Locale.setDefault(Locale.forLanguageTag("es-ES"))
    }

    private val cloudState = MainUiState(
        user = TestData.emil,
        isCloud = true,
        groups = listOf(TestData.group, TestData.otherGroup),
        groupsLoaded = true,
        selectedGroup = TestData.group,
        movements = TestData.movements,
        movementsLoaded = true,
        themeMode = ThemeMode.DARK,
    )

    private fun shot(name: String, dark: Boolean = true, content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalToday provides TestData.today) {
                MiGastoTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Box(Modifier.fillMaxSize()) { content() }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    @Test
    fun home() = shot("01_home_dark") { MainScreen(cloudState, MainActions()) }

    @Test
    fun homeLight() = shot("02_home_light", dark = false) {
        MainScreen(cloudState.copy(themeMode = ThemeMode.LIGHT), MainActions())
    }

    @Test
    fun calendar() = shot("03_calendar_dark") { MainScreen(cloudState, MainActions(), initialTab = MainTab.CALENDAR) }

    @Test
    fun calendarDayWithMovements() {
        shot("04_calendar_day1") { MainScreen(cloudState, MainActions(), initialTab = MainTab.CALENDAR) }
        compose.onNodeWithText("1", useUnmergedTree = true).performClick()
        compose.onRoot().captureRoboImage("build/screenshots/04_calendar_day1.png")
    }

    @Test
    fun stats() = shot("05_stats_dark") { MainScreen(cloudState, MainActions(), initialTab = MainTab.STATS) }

    @Test
    fun statsLight() = shot("06_stats_light", dark = false) {
        MainScreen(cloudState, MainActions(), initialTab = MainTab.STATS)
    }

    @Test
    @Config(qualifiers = "es-rES-w411dp-h2400dp-xxhdpi")
    fun statsFull() = shot("05b_stats_full") { MainScreen(cloudState, MainActions(), initialTab = MainTab.STATS) }

    @Test
    @Config(qualifiers = "es-rES-w411dp-h2400dp-xxhdpi")
    fun statsYearFull() = shot("05c_stats_year_light", dark = false) {
        StatsScreen(cloudState, initialPeriod = StatsPeriod.YEAR)
    }

    @Test
    fun transferEditor() = shot("09b_transfer") {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            MovementEditorContent(
                existing = TestData.movements.first { it.description == "Cajero" },
                initialDate = TestData.today,
                loadPhoto = { null },
                onSave = { _, _, _, _ -> },
                onDismiss = {},
            )
        }
    }

    @Test
    fun categoryPicker() {
        shot("09c_category_picker") {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                MovementEditorContent(
                    existing = null,
                    initialDate = TestData.today,
                    loadPhoto = { null },
                    onSave = { _, _, _, _ -> },
                    onDismiss = {},
                )
            }
        }
        compose.onNodeWithText("Categoría").performClick()
        compose.waitForIdle()
        captureScreenRoboImage("build/screenshots/09c_category_picker.png")
    }

    @Test
    fun settings() = shot("07_settings_dark") { MainScreen(cloudState, MainActions(), initialTab = MainTab.SETTINGS) }

    @Test
    @Config(qualifiers = "es-rES-w411dp-h1800dp-xxhdpi")
    fun settingsFull() = shot("07b_settings_full") { MainScreen(cloudState, MainActions(), initialTab = MainTab.SETTINGS) }

    @Test
    fun settingsLocal() = shot("08_settings_local_light", dark = false) {
        MainScreen(cloudState.copy(isCloud = false, themeMode = ThemeMode.LIGHT), MainActions(), initialTab = MainTab.SETTINGS)
    }

    @Test
    fun editor() = shot("09_add_movement") {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            MovementEditorContent(
                existing = null,
                initialDate = TestData.today,
                loadPhoto = { null },
                onSave = { _, _, _, _ -> },
                onDismiss = {},
            )
        }
    }

    @Test
    fun editorExisting() = shot("10_edit_movement_light", dark = false) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            MovementEditorContent(
                existing = TestData.movements.first { it.description == "Luz + Agua" },
                initialDate = TestData.today,
                loadPhoto = { null },
                onSave = { _, _, _, _ -> },
                onDismiss = {},
            )
        }
    }

    @Test
    fun groupSettings() {
        shot("11_group_settings") {
            MainScreen(cloudState, MainActions(), initialTab = MainTab.SETTINGS)
            GroupSettingsDialog(
                group = TestData.group,
                currentUserId = TestData.emil.uid,
                isCloud = true,
                onEdit = {},
                onLeave = {},
                onDismiss = {},
            )
        }
        captureScreenRoboImage("build/screenshots/11_group_settings.png")
    }

    @Test
    fun removeMember() {
        shot("11b_remove_member") {
            MainScreen(cloudState, MainActions(), initialTab = MainTab.SETTINGS)
            RemoveMemberDialog(
                member = TestData.group.members.last(),
                movementCount = 3,
                groupOpen = true,
                onDismiss = {},
                onConfirm = {},
            )
        }
        captureScreenRoboImage("build/screenshots/11b_remove_member.png")
    }

    @Test
    fun groupsSheet() {
        shot("12_groups_sheet") { MainScreen(cloudState, MainActions()) }
        compose.onNodeWithText("Casa").performClick()
        compose.waitForIdle()
        captureScreenRoboImage("build/screenshots/12_groups_sheet.png")
    }

    @Test
    fun movementDetail() {
        shot("13_movement_detail") { MainScreen(cloudState, MainActions()) }
        compose.onNodeWithText("Finiquito Emil").performClick()
        compose.waitForIdle()
        captureScreenRoboImage("build/screenshots/13_movement_detail.png")
    }

    @Test
    fun emptyGroups() = shot("14_no_groups") {
        MainScreen(cloudState.copy(groups = emptyList(), selectedGroup = null, movements = emptyList()), MainActions())
    }

    @Test
    fun emptyHome() = shot("15_empty_home_light", dark = false) {
        MainScreen(cloudState.copy(movements = emptyList()), MainActions())
    }

    @Test
    fun login() = shot("16_login") {
        AuthScreen(
            state = AuthUiState(email = "emil@example.com", password = "secreto"),
            onNameChange = {}, onEmailChange = {}, onPasswordChange = {},
            onToggleMode = {}, onSubmit = {}, onForgotPassword = {},
            showGoogle = true,
        )
    }

    @Test
    fun register() = shot("17_register_light", dark = false) {
        AuthScreen(
            state = AuthUiState(registering = true, name = "Emil"),
            onNameChange = {}, onEmailChange = {}, onPasswordChange = {},
            onToggleMode = {}, onSubmit = {}, onForgotPassword = {},
            showGoogle = true,
        )
    }

    @Test
    fun welcome() = shot("18_welcome_local") {
        WelcomeScreen(state = AuthUiState(groupName = "Casa"), onNameChange = {}, onGroupNameChange = {}, onStart = {})
    }

    @Test
    fun narrowScreenLongValues() {
        val big = TestData.movements.map { if (it.description == "Finiquito Emil") it.copy(amountCents = 1_234_567_89, description = "Una descripción larguísima que no cabe en una línea") else it }
        compose.setContent {
            CompositionLocalProvider(LocalToday provides TestData.today) {
                MiGastoTheme(ThemeMode.DARK) {
                    MainScreen(cloudState.copy(movements = big), MainActions())
                }
            }
        }
        compose.onRoot().captureRoboImage("build/screenshots/19_long_values.png")
    }
}
