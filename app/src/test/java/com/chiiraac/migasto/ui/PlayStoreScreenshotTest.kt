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
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chiiraac.migasto.TestData
import com.chiiraac.migasto.data.model.ThemeMode
import com.chiiraac.migasto.ui.components.LocalToday
import com.chiiraac.migasto.ui.groups.GroupSettingsDialog
import com.chiiraac.migasto.ui.main.MainActions
import com.chiiraac.migasto.ui.main.MainScreen
import com.chiiraac.migasto.ui.main.MainTab
import com.chiiraac.migasto.ui.main.MainUiState
import com.chiiraac.migasto.ui.movement.MovementEditorContent
import com.chiiraac.migasto.ui.theme.MiGastoTheme
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import java.util.Locale
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Capturas en proporción 9:16 para la ficha de Google Play (build/play-store/raw).
 * Después se componen con texto mediante tools/play-store-assets.mjs.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es-rES-w411dp-h731dp-xxhdpi")
@OptIn(ExperimentalRoborazziApi::class)
class PlayStoreScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setLocale() {
        Locale.setDefault(Locale.forLanguageTag("es-ES"))
    }

    private val state = MainUiState(
        user = TestData.emil,
        isCloud = true,
        groups = listOf(TestData.group, TestData.otherGroup),
        groupsLoaded = true,
        selectedGroup = TestData.group,
        movements = TestData.movements,
        movementsLoaded = true,
        themeMode = ThemeMode.DARK,
    )

    private fun render(dark: Boolean = true, content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalToday provides TestData.today) {
                MiGastoTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Box(Modifier.fillMaxSize()) { content() }
                    }
                }
            }
        }
    }

    private fun save(name: String) {
        compose.waitForIdle()
        captureScreenRoboImage("build/play-store/raw/$name.png")
    }

    @Test
    fun home() {
        render { MainScreen(state, MainActions()) }
        save("1_home")
    }

    @Test
    fun addMovement() {
        render {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                MovementEditorContent(
                    existing = TestData.movements.first { it.description == "Mercadona" }.copy(hasPhoto = false),
                    initialDate = TestData.today,
                    loadPhoto = { null },
                    onSave = { _, _, _, _ -> },
                    onDismiss = {},
                )
            }
        }
        save("2_add")
    }

    @Test
    fun calendar() {
        render { MainScreen(state, MainActions(), initialTab = MainTab.CALENDAR) }
        compose.onNodeWithText("1", useUnmergedTree = true).performClick()
        save("3_calendar")
    }

    @Test
    fun stats() {
        render { MainScreen(state, MainActions(), initialTab = MainTab.STATS) }
        save("4_stats")
    }

    @Test
    fun groups() {
        render {
            MainScreen(state, MainActions())
            GroupSettingsDialog(
                group = TestData.group,
                currentUserId = TestData.emil.uid,
                isCloud = true,
                onEdit = {},
                onLeave = {},
                onDismiss = {},
            )
        }
        save("5_group")
    }

    @Test
    fun homeLight() {
        render(dark = false) { MainScreen(state.copy(themeMode = ThemeMode.LIGHT), MainActions()) }
        save("6_light")
    }
}
