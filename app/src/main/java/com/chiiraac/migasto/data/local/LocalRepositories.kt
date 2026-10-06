package com.chiiraac.migasto.data.local

import com.chiiraac.migasto.data.AppError
import com.chiiraac.migasto.data.InviteCodes
import com.chiiraac.migasto.data.model.Group
import com.chiiraac.migasto.data.model.GroupIcon
import com.chiiraac.migasto.data.model.Member
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
import com.chiiraac.migasto.data.runCatchingApp
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Modo local: no hay cuentas, solo un perfil guardado en el dispositivo. */
class LocalAuthRepository(
    private val preferences: UserPreferences,
    scope: CoroutineScope,
) : AuthRepository {

    override val authState: StateFlow<AuthState> = preferences.localProfile
        .map { profile ->
            if (profile == null) AuthState.SignedOut
            else AuthState.SignedIn(UserProfile(profile.uid, profile.name, email = null))
        }
        .stateIn(scope, SharingStarted.Eagerly, AuthState.Loading)

    override suspend fun startLocal(name: String): Result<UserProfile> = runCatchingApp {
        val existing = preferences.currentLocalProfile()
        val profile = UserPreferences.LocalProfile(
            uid = existing?.uid ?: UUID.randomUUID().toString(),
            name = name.trim(),
        )
        preferences.saveLocalProfile(profile)
        UserProfile(profile.uid, profile.name, null)
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> = notAvailable()

    override suspend fun register(name: String, email: String, password: String): Result<Unit> = notAvailable()

    override suspend fun sendPasswordReset(email: String): Result<Unit> = notAvailable()

    override suspend fun updateName(name: String): Result<UserProfile> = startLocal(name)

    override suspend fun reauthenticate(password: String): Result<Unit> = Result.success(Unit)

    override suspend fun signOut() = Unit

    override suspend fun deleteAccount(): Result<Unit> = runCatchingApp {
        preferences.clearAccountData()
    }

    private fun <T> notAvailable(): Result<T> =
        Result.failure(AppError(AppError.Reason.NOT_AVAILABLE_OFFLINE_MODE))
}

/** Modo local: grupos y movimientos en Room, fotos en ficheros privados. */
class LocalFinanceRepository(
    private val database: LocalDatabase,
    private val photos: LocalPhotoStore,
    private val clock: () -> Long = System::currentTimeMillis,
) : FinanceRepository {

    private val groupDao get() = database.groupDao()
    private val movementDao get() = database.movementDao()

    override fun observeGroups(user: UserProfile): Flow<List<Group>> =
        groupDao.observeAll().map { groups -> groups.map { it.toModel(user) } }

    override fun observeMovements(groupId: String): Flow<List<Movement>> =
        movementDao.observeByGroup(groupId).map { list -> list.map { it.toModel() } }

    override suspend fun createGroup(user: UserProfile, name: String, icon: GroupIcon): Result<Group> =
        runCatchingApp {
            val entity = GroupEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                icon = icon.name,
                inviteCode = InviteCodes.generate(),
                createdAt = clock(),
            )
            groupDao.upsert(entity)
            entity.toModel(user)
        }

    override suspend fun joinGroup(user: UserProfile, inviteCode: String): Result<Group> =
        Result.failure(AppError(AppError.Reason.NOT_AVAILABLE_OFFLINE_MODE))

    override suspend fun leaveGroup(user: UserProfile, group: Group): Result<Unit> = runCatchingApp {
        deleteGroupWithPhotos(group.id)
    }

    override suspend fun updateGroup(group: Group, name: String, icon: GroupIcon): Result<Unit> =
        runCatchingApp {
            val current = groupDao.get(group.id) ?: return@runCatchingApp
            groupDao.upsert(current.copy(name = name.trim(), icon = icon.name))
        }

    override suspend fun saveMovement(
        user: UserProfile,
        groupId: String,
        movementId: String?,
        draft: MovementDraft,
        photo: PhotoChange,
    ): Result<Unit> = runCatchingApp {
        val existing = movementId?.let { movementDao.get(it) }
        val id = existing?.id ?: UUID.randomUUID().toString()
        val hasPhoto = when (photo) {
            PhotoChange.Keep -> existing?.hasPhoto ?: false
            PhotoChange.Remove -> false
            is PhotoChange.Replace -> true
        }
        when (photo) {
            PhotoChange.Keep -> Unit
            PhotoChange.Remove -> photos.delete(id)
            is PhotoChange.Replace -> photos.save(id, photo.jpegBytes)
        }
        movementDao.upsert(
            MovementEntity(
                id = id,
                groupId = groupId,
                type = draft.type.name,
                method = draft.method.name,
                amountCents = draft.amountCents,
                description = draft.description.trim(),
                categoryId = draft.categoryId,
                dateEpochDay = draft.date.toEpochDay(),
                createdAt = existing?.createdAt ?: clock(),
                createdById = existing?.createdById ?: user.uid,
                createdByName = existing?.createdByName ?: user.name,
                hasPhoto = hasPhoto,
            ),
        )
    }

    override suspend fun deleteMovement(movement: Movement): Result<Unit> = runCatchingApp {
        movementDao.delete(movement.id)
        photos.delete(movement.id)
    }

    override suspend fun loadPhoto(movement: Movement): ByteArray? = photos.load(movement.id)

    override suspend fun updateMemberProfile(user: UserProfile): Result<Unit> = runCatchingApp {
        movementDao.renameAuthor(user.uid, user.name)
    }

    override suspend fun purgeUser(user: UserProfile): Result<Unit> = runCatchingApp {
        groupDao.deleteAll()
        photos.deleteAll()
    }

    private suspend fun deleteGroupWithPhotos(groupId: String) {
        movementDao.photoIdsInGroup(groupId).forEach { photos.delete(it) }
        groupDao.delete(groupId)
    }
}

private fun GroupEntity.toModel(user: UserProfile) = Group(
    id = id,
    name = name,
    icon = GroupIcon.fromKey(icon),
    inviteCode = inviteCode,
    ownerId = user.uid,
    members = listOf(Member(user.uid, user.name, user.email)),
    createdAt = createdAt,
)

private fun MovementEntity.toModel() = Movement(
    id = id,
    groupId = groupId,
    type = MovementType.fromKey(type),
    method = PaymentMethod.fromKey(method),
    amountCents = amountCents,
    description = description,
    categoryId = categoryId,
    date = LocalDate.ofEpochDay(dateEpochDay),
    createdAt = createdAt,
    createdById = createdById,
    createdByName = createdByName,
    hasPhoto = hasPhoto,
)
