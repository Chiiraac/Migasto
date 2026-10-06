package com.chiiraac.migasto.ui.main

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chiiraac.migasto.TestData
import com.chiiraac.migasto.data.AppError
import com.chiiraac.migasto.data.model.Group
import com.chiiraac.migasto.data.model.GroupIcon
import com.chiiraac.migasto.data.model.Movement
import com.chiiraac.migasto.data.model.MovementDraft
import com.chiiraac.migasto.data.model.MovementType
import com.chiiraac.migasto.data.model.PaymentMethod
import com.chiiraac.migasto.data.model.PhotoChange
import com.chiiraac.migasto.data.model.UserProfile
import com.chiiraac.migasto.data.prefs.UserPreferences
import com.chiiraac.migasto.data.repository.AuthRepository
import com.chiiraac.migasto.data.repository.AuthState
import com.chiiraac.migasto.data.repository.FinanceRepository
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MainViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakeAuth(user: UserProfile) : AuthRepository {
        override val authState: StateFlow<AuthState> = MutableStateFlow(AuthState.SignedIn(user))
        override suspend fun startLocal(name: String) = Result.success(TestData.emil)
        override suspend fun signIn(email: String, password: String) = Result.success(Unit)
        override suspend fun register(name: String, email: String, password: String) = Result.success(Unit)
        override suspend fun sendPasswordReset(email: String) = Result.success(Unit)
        override suspend fun updateName(name: String) = Result.success(TestData.emil)
        override suspend fun reauthenticate(password: String) = Result.success(Unit)
        override suspend fun signOut() = Unit
        override suspend fun deleteAccount() = Result.success(Unit)
    }

    /** La primera escucha de movimientos falla (grupo aún no confirmado en el servidor). */
    private class FlakyFinance(private val movements: List<Movement>) : FinanceRepository {
        var movementSubscriptions = 0
        val saved = mutableListOf<MovementDraft>()
        override fun observeGroups(user: UserProfile): Flow<List<Group>> = flowOf(listOf(TestData.group))
        override fun observeMovements(groupId: String): Flow<List<Movement>> = flow {
            movementSubscriptions++
            if (movementSubscriptions == 1) throw AppError(AppError.Reason.PERMISSION_DENIED)
            emit(movements)
        }
        override suspend fun createGroup(user: UserProfile, name: String, icon: GroupIcon) = Result.success(TestData.group)
        override suspend fun joinGroup(user: UserProfile, inviteCode: String) = Result.success(TestData.group)
        override suspend fun leaveGroup(user: UserProfile, group: Group) = Result.success(Unit)
        override suspend fun updateGroup(group: Group, name: String, icon: GroupIcon) = Result.success(Unit)
        override suspend fun saveMovement(
            user: UserProfile, groupId: String, movementId: String?, draft: MovementDraft, photo: PhotoChange,
        ): Result<Unit> {
            saved += draft
            return Result.success(Unit)
        }
        override suspend fun deleteMovement(movement: Movement) = Result.success(Unit)
        override suspend fun loadPhoto(movement: Movement): ByteArray? = null
        override suspend fun updateMemberProfile(user: UserProfile) = Result.success(Unit)
        override suspend fun purgeUser(user: UserProfile) = Result.success(Unit)
    }

    private fun viewModel(finance: FinanceRepository): MainViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val file = File(app.filesDir, "test-${System.nanoTime()}.preferences_pb")
        val prefs = UserPreferences(PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { file })
        return MainViewModel(app, FakeAuth(TestData.emil), finance, prefs, isCloud = true)
    }

    @Test
    fun movementsListenerIsRetriedAfterAnError() = scope.runTest {
        val finance = FlakyFinance(TestData.movements)
        val vm = viewModel(finance)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals(2, finance.movementSubscriptions)
        assertTrue(state.movementsLoaded)
        assertEquals(TestData.movements.size, state.movements.size)
    }

    @Test
    fun savingReportsTheEditorTokenSoTheEditorCloses() = scope.runTest {
        val finance = FlakyFinance(TestData.movements)
        val vm = viewModel(finance)
        advanceUntilIdle()
        var done: Boolean? = null
        val draft = MovementDraft(MovementType.EXPENSE, PaymentMethod.BANK, 500, "Café", "restaurants", LocalDate.of(2026, 10, 6))
        vm.saveMovement(editorToken = 7, movementId = null, draft = draft, newPhoto = null, removePhoto = false) { done = it }
        advanceUntilIdle()
        assertEquals(true, done)
        assertEquals(7, vm.uiState.value.savedEditorToken)
        assertEquals(listOf(draft), finance.saved)
    }
}
