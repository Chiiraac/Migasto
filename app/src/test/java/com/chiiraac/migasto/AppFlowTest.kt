package com.chiiraac.migasto

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import java.util.Locale
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Prueba de extremo a extremo con la app real en modo local (Room + DataStore + ViewModels):
 * bienvenida → añadir un gasto → aparece en la lista y el saldo cambia → editarlo → borrarlo.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es-rES-w411dp-h891dp-xxhdpi")
@OptIn(ExperimentalTestApi::class)
class AppFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun setLocale() {
        Locale.setDefault(Locale.forLanguageTag("es-ES"))
    }

    private fun waitFor(text: String) = compose.waitUntilAtLeastOneExists(hasText(text, substring = true), 15_000)

    @Test
    fun welcomeAddEditDeleteMovement() {
        // Bienvenida (modo local)
        waitFor("Bienvenido a MiGasto")
        compose.onNodeWithText("¿Cómo te llamas?").performTextInput("Emil")
        compose.onNodeWithText("Empezar").performClick()

        // Pantalla principal con el grupo por defecto
        waitFor("Balance Total")
        waitFor("Aún no hay movimientos")
        compose.onNodeWithText("Casa").assertExists()

        // Añadir un gasto en efectivo
        compose.onNodeWithContentDescription("Añadir movimiento").performClick()
        waitFor("Añadir Movimiento")
        compose.onNode(hasText("Efectivo") and isRadioButton()).performClick()
        compose.onNodeWithText("Cantidad (€)").performTextInput("12,5")
        compose.onNodeWithText("Descripción").performTextInput("Panadería")
        compose.onNodeWithText("Categoría").performClick()
        waitFor("Elige una categoría")
        compose.onNodeWithText("Supermercado").performClick()
        compose.onNodeWithText("Guardar Movimiento").performClick()

        waitFor("Panadería")
        waitFor("-12,50")
        compose.onRoot().captureRoboImage("build/screenshots/20_flow_after_add.png")

        // Detalle → editar el importe
        compose.onNodeWithText("Panadería").performClick()
        waitFor("Editar")
        compose.onNodeWithText("Editar").performClick()
        waitFor("Editar Movimiento")
        compose.onNodeWithText("12,50").assertExists()
        compose.onNodeWithText("Cantidad (€)").performTextReplacementSafe("20")
        compose.onNodeWithText("Guardar Movimiento").performClick()
        waitFor("-20,00")

        // Detalle → eliminar
        compose.onNodeWithText("Panadería").performClick()
        waitFor("Editar")
        compose.onNodeWithText("Eliminar").performClick()
        waitFor("¿Eliminar este movimiento?")
        compose.onAllNodes(hasText("Eliminar")).let { nodes ->
            // El botón de confirmación del diálogo es el último "Eliminar"
            nodes[nodes.fetchSemanticsNodes().size - 1].performClick()
        }
        waitFor("Aún no hay movimientos")
    }

    private fun isRadioButton() = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.performTextReplacementSafe(text: String) {
        performTextClearance()
        performTextInput(text)
    }
}
