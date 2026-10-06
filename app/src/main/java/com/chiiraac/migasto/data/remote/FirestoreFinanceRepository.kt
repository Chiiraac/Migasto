package com.chiiraac.migasto.data.remote

import android.util.Log
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
import com.chiiraac.migasto.data.repository.FinanceRepository
import com.chiiraac.migasto.data.runCatchingApp
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import java.time.LocalDate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Modo nube con Cloud Firestore.
 *
 * Estructura:
 * - `groups/{groupId}`: nombre, icono, código, `memberIds` y `members` (nombre/email de cada miembro).
 * - `groups/{groupId}/movements/{movementId}`: movimientos del grupo.
 * - `groups/{groupId}/photos/{movementId}`: foto del ticket comprimida (JPEG, < 1 MB).
 * - `inviteCodes/{code}`: código de invitación → `groupId`.
 *
 * Las fotos se guardan en Firestore (y no en Cloud Storage) para que el proyecto
 * funcione con el plan gratuito de Firebase. Las reglas de seguridad están en `firestore.rules`.
 */
class FirestoreFinanceRepository(
    private val db: FirebaseFirestore,
    private val clock: () -> Long = System::currentTimeMillis,
) : FinanceRepository {

    private val groups = db.collection(GROUPS)
    private val inviteCodes = db.collection(INVITE_CODES)

    private fun movementsOf(groupId: String) = groups.document(groupId).collection(MOVEMENTS)
    private fun photosOf(groupId: String) = groups.document(groupId).collection(PHOTOS)

    override fun observeGroups(user: UserProfile): Flow<List<Group>> =
        groups.whereArrayContains(F_MEMBER_IDS, user.uid)
            .snapshotFlow()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toGroup() }.sortedBy { it.createdAt }
            }

    override fun observeMovements(groupId: String): Flow<List<Movement>> =
        movementsOf(groupId)
            .snapshotFlow()
            .map { snapshot ->
                snapshot.documents
                    .mapNotNull { it.toMovement(groupId) }
                    .sortedWith(compareByDescending<Movement> { it.date }.thenByDescending { it.createdAt })
            }

    override suspend fun createGroup(user: UserProfile, name: String, icon: GroupIcon): Result<Group> =
        firestoreCall {
            val ref = groups.document()
            val code = InviteCodes.generate()
            val createdAt = clock()
            val batch = db.batch()
            batch.set(
                ref,
                mapOf(
                    F_NAME to name.trim(),
                    F_ICON to icon.name,
                    F_INVITE_CODE to code,
                    F_OWNER_ID to user.uid,
                    F_MEMBER_IDS to listOf(user.uid),
                    F_MEMBERS to mapOf(user.uid to memberData(user)),
                    F_CREATED_AT to createdAt,
                ),
            )
            batch.set(inviteCodes.document(code), mapOf(F_GROUP_ID to ref.id, F_CREATED_BY to user.uid))
            batch.commit().awaitOrQueued()
            Group(
                id = ref.id,
                name = name.trim(),
                icon = icon,
                inviteCode = code,
                ownerId = user.uid,
                members = listOf(Member(user.uid, user.name, user.email)),
                createdAt = createdAt,
            )
        }

    override suspend fun joinGroup(user: UserProfile, inviteCode: String): Result<Group> = firestoreCall {
        val code = InviteCodes.normalize(inviteCode)
        if (!InviteCodes.isValid(code)) throw AppError(AppError.Reason.INVALID_INVITE_CODE)

        val codeSnapshot = inviteCodes.document(code).get(Source.SERVER).await()
        val groupId = codeSnapshot.getString(F_GROUP_ID)
            ?: throw AppError(AppError.Reason.INVALID_INVITE_CODE)
        val groupRef = groups.document(groupId)

        try {
            // Con un límite de tiempo: sin conexión Firestore nunca falla la escritura, la deja en cola.
            groupRef.update(
                mapOf(
                    F_MEMBER_IDS to FieldValue.arrayUnion(user.uid),
                    "$F_MEMBERS.${user.uid}" to memberData(user),
                    F_JOIN_CODE to code,
                ),
            ).awaitWrite(confirmed = true)
        } catch (e: Exception) {
            val mapped = FirebaseErrors.map(e)
            if (mapped is AppError && mapped.reason == AppError.Reason.PERMISSION_DENIED) {
                throw AppError(AppError.Reason.INVALID_INVITE_CODE, e)
            }
            throw mapped
        }
        groupRef.get(Source.SERVER).await().toGroup()
            ?: throw AppError(AppError.Reason.INVALID_INVITE_CODE)
    }

    override suspend fun leaveGroup(user: UserProfile, group: Group): Result<Unit> = firestoreCall {
        leave(user, group, confirmed = false)
    }

    /**
     * Sale de [group]; si era el último miembro, lo borra entero. Con [confirmed] cada escritura
     * espera la confirmación del servidor (se usa al borrar la cuenta, cuando ya no habrá sesión
     * para enviar después lo que quede pendiente).
     *
     * Quién queda en el grupo se decide con el estado del servidor dentro de una transacción, no
     * con la copia local (que puede ir con retraso): si alguien acaba de unirse, el grupo no se borra.
     */
    private suspend fun leave(user: UserProfile, group: Group, confirmed: Boolean) {
        val ref = groups.document(group.id)
        val lastMember = db.runTransaction { tx ->
            val snapshot = tx.get(ref)
            val ids = snapshot.memberIds()
            when {
                !snapshot.exists() || user.uid !in ids -> false
                ids.size == 1 -> true
                else -> {
                    tx.update(ref, removeMemberUpdates(user.uid, snapshot, ids))
                    false
                }
            }
        }.awaitServer()
        if (lastMember) deleteGroupCompletely(user, group, confirmed)
    }

    private fun removeMemberUpdates(uid: String, snapshot: DocumentSnapshot, ids: List<String>): Map<String, Any> {
        val updates = mutableMapOf<String, Any>(
            F_MEMBER_IDS to FieldValue.arrayRemove(uid),
            "$F_MEMBERS.$uid" to FieldValue.delete(),
        )
        if (snapshot.getString(F_OWNER_ID) == uid) updates[F_OWNER_ID] = ids.first { it != uid }
        return updates
    }

    override suspend fun updateGroup(group: Group, name: String, icon: GroupIcon): Result<Unit> =
        firestoreCall {
            groups.document(group.id)
                .update(mapOf(F_NAME to name.trim(), F_ICON to icon.name))
                .awaitOrQueued()
        }

    override suspend fun saveMovement(
        user: UserProfile,
        groupId: String,
        movementId: String?,
        draft: MovementDraft,
        photo: PhotoChange,
    ): Result<Unit> = firestoreCall {
        val collection = movementsOf(groupId)
        val ref = movementId?.let { collection.document(it) } ?: collection.document()
        val data = mutableMapOf<String, Any>(
            F_TYPE to draft.type.name,
            F_METHOD to draft.method.name,
            F_AMOUNT_CENTS to draft.amountCents,
            F_DESCRIPTION to draft.description.trim(),
            F_CATEGORY_ID to draft.categoryId,
            F_DATE_EPOCH_DAY to draft.date.toEpochDay(),
            F_UPDATED_AT to clock(),
        )
        if (movementId == null) {
            data[F_CREATED_AT] = clock()
            data[F_CREATED_BY_ID] = user.uid
            data[F_CREATED_BY_NAME] = user.name
            data[F_HAS_PHOTO] = photo is PhotoChange.Replace
        } else if (photo !is PhotoChange.Keep) {
            data[F_HAS_PHOTO] = photo is PhotoChange.Replace
        }

        val batch = db.batch()
        batch.set(ref, data, SetOptions.merge())
        val photoRef = photosOf(groupId).document(ref.id)
        when (photo) {
            PhotoChange.Keep -> Unit
            PhotoChange.Remove -> batch.delete(photoRef)
            is PhotoChange.Replace -> {
                if (photo.jpegBytes.size > MAX_PHOTO_BYTES) throw AppError(AppError.Reason.PHOTO_TOO_LARGE)
                batch.set(
                    photoRef,
                    mapOf(F_DATA to Blob.fromBytes(photo.jpegBytes), F_CREATED_BY to user.uid),
                )
            }
        }
        batch.commit().awaitOrQueued()
    }

    override suspend fun deleteMovement(movement: Movement): Result<Unit> = firestoreCall {
        val batch = db.batch()
        batch.delete(movementsOf(movement.groupId).document(movement.id))
        if (movement.hasPhoto) batch.delete(photosOf(movement.groupId).document(movement.id))
        batch.commit().awaitOrQueued()
    }

    override suspend fun loadPhoto(movement: Movement): ByteArray? =
        runCatchingApp {
            photosOf(movement.groupId).document(movement.id).get().await().getBlob(F_DATA)?.toBytes()
        }.getOrNull()

    override suspend fun updateMemberProfile(user: UserProfile): Result<Unit> = firestoreCall {
        val myGroups = groups.whereArrayContains(F_MEMBER_IDS, user.uid).get().await()
        if (myGroups.isEmpty) return@firestoreCall
        val batch = db.batch()
        myGroups.documents.forEach { doc ->
            batch.update(doc.reference, "$F_MEMBERS.${user.uid}", memberData(user))
        }
        batch.commit().awaitOrQueued()
    }

    override suspend fun purgeUser(user: UserProfile): Result<Unit> = firestoreCall {
        val myGroups = groups.whereArrayContains(F_MEMBER_IDS, user.uid)
            .get(Source.SERVER).await()
            .documents.mapNotNull { it.toGroup() }
        myGroups.forEach { group -> leave(user, group, confirmed = true) }
        // Cualquier otra escritura pendiente de este usuario debe llegar antes de borrar la cuenta.
        db.waitForPendingWrites().awaitWrite(confirmed = true)
    }

    override suspend fun flushPendingWrites(timeoutMs: Long): Boolean =
        withTimeoutOrNull(timeoutMs) {
            // Si alguna escritura pendiente se rechaza, ya no queda nada por enviar.
            runCatching { db.waitForPendingWrites().await() }
            true
        } ?: false

    /** Borra movimientos, fotos, código de invitación y el propio grupo. */
    private suspend fun deleteGroupCompletely(user: UserProfile, group: Group, confirmed: Boolean) {
        // Lectura del servidor (no de la caché) para no dejar documentos huérfanos. Las fotos
        // usan el mismo id que su movimiento, así que no hace falta descargarlas para borrarlas.
        val movementDocs = movementsOf(group.id).get(Source.SERVER).await().documents
        val children = mutableListOf<DocumentReference>()
        movementDocs.forEach { doc ->
            children += doc.reference
            if (doc.getBoolean(F_HAS_PHOTO) == true) children += photosOf(group.id).document(doc.id)
        }
        children.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.delete(it) }
            batch.commit().awaitWrite(confirmed)
        }
        // Último paso, otra vez contra el servidor: si mientras tanto alguien se ha unido,
        // el grupo se conserva para esa persona y solo se sale de él.
        val ref = groups.document(group.id)
        db.runTransaction { tx ->
            val snapshot = tx.get(ref)
            val ids = snapshot.memberIds()
            if (snapshot.exists() && user.uid in ids) {
                if (ids.size == 1) {
                    snapshot.getString(F_INVITE_CODE)?.takeIf { it.isNotBlank() }
                        ?.let { tx.delete(inviteCodes.document(it)) }
                    tx.delete(ref)
                } else {
                    tx.update(ref, removeMemberUpdates(user.uid, snapshot, ids))
                }
            }
        }.awaitServer()
    }

    private fun DocumentSnapshot.memberIds(): List<String> =
        (get(F_MEMBER_IDS) as? List<*>).orEmpty().filterIsInstance<String>()

    private fun memberData(user: UserProfile): Map<String, Any?> =
        mapOf(F_NAME to user.name, F_EMAIL to user.email)

    private inline fun <T> firestoreCall(block: () -> T): Result<T> =
        runCatchingApp(block).recoverCatching { throw FirebaseErrors.map(it) }

    companion object {
        private const val TAG = "Firestore"
        const val MAX_PHOTO_BYTES = 900 * 1024
        private const val BATCH_LIMIT = 400
        private const val WRITE_TIMEOUT_MS = 4_000L
        private const val CONFIRMED_WRITE_TIMEOUT_MS = 30_000L

        const val GROUPS = "groups"
        const val MOVEMENTS = "movements"
        const val PHOTOS = "photos"
        const val INVITE_CODES = "inviteCodes"

        const val F_NAME = "name"
        const val F_EMAIL = "email"
        const val F_ICON = "icon"
        const val F_INVITE_CODE = "inviteCode"
        const val F_OWNER_ID = "ownerId"
        const val F_MEMBER_IDS = "memberIds"
        const val F_MEMBERS = "members"
        const val F_CREATED_AT = "createdAt"
        const val F_UPDATED_AT = "updatedAt"
        const val F_JOIN_CODE = "joinCode"
        const val F_GROUP_ID = "groupId"
        const val F_CREATED_BY = "createdBy"
        const val F_TYPE = "type"
        const val F_METHOD = "method"
        const val F_AMOUNT_CENTS = "amountCents"
        const val F_DESCRIPTION = "description"
        const val F_CATEGORY_ID = "categoryId"
        const val F_DATE_EPOCH_DAY = "dateEpochDay"
        const val F_CREATED_BY_ID = "createdById"
        const val F_CREATED_BY_NAME = "createdByName"
        const val F_HAS_PHOTO = "hasPhoto"
        const val F_DATA = "data"

        /** Espera la confirmación del servidor si [confirmed]; si no, como [awaitOrQueued]. */
        private suspend fun Task<*>.awaitWrite(confirmed: Boolean) {
            if (!confirmed) return awaitOrQueued()
            withTimeoutOrNull(CONFIRMED_WRITE_TIMEOUT_MS) { await(); true }
                ?: throw AppError(AppError.Reason.NETWORK)
        }

        /** Resultado de una transacción (siempre va contra el servidor), con límite de tiempo. */
        private suspend fun <T> Task<T>.awaitServer(): T =
            withTimeoutOrNull(CONFIRMED_WRITE_TIMEOUT_MS) { listOf(await()) }?.single()
                ?: throw AppError(AppError.Reason.NETWORK)

        /**
         * Espera la confirmación del servidor un tiempo prudencial. Sin conexión, Firestore
         * guarda la escritura en local y la sincroniza después, así que no bloqueamos la interfaz.
         */
        private suspend fun Task<*>.awaitOrQueued() {
            val finished = withTimeoutOrNull(WRITE_TIMEOUT_MS) { await(); true }
            if (finished == null) {
                addOnFailureListener { Log.w(TAG, "Escritura pendiente rechazada", it) }
            }
        }
    }
}

private fun Query.snapshotFlow(): Flow<QuerySnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(FirebaseErrors.map(error))
            return@addSnapshotListener
        }
        if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

private fun DocumentSnapshot.toGroup(): Group? {
    if (!exists()) return null
    val memberIds = (get(FirestoreFinanceRepository.F_MEMBER_IDS) as? List<*>)
        ?.filterIsInstance<String>()
        .orEmpty()
    val membersData = get(FirestoreFinanceRepository.F_MEMBERS) as? Map<*, *> ?: emptyMap<Any, Any>()
    val members = memberIds.map { uid ->
        val data = membersData[uid] as? Map<*, *>
        val email = data?.get(FirestoreFinanceRepository.F_EMAIL) as? String
        val name = (data?.get(FirestoreFinanceRepository.F_NAME) as? String)
            ?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore('@')
            ?: ""
        Member(uid = uid, name = name, email = email)
    }
    return Group(
        id = id,
        name = getString(FirestoreFinanceRepository.F_NAME).orEmpty(),
        icon = GroupIcon.fromKey(getString(FirestoreFinanceRepository.F_ICON)),
        inviteCode = getString(FirestoreFinanceRepository.F_INVITE_CODE).orEmpty(),
        ownerId = getString(FirestoreFinanceRepository.F_OWNER_ID).orEmpty(),
        members = members,
        createdAt = getLong(FirestoreFinanceRepository.F_CREATED_AT) ?: 0L,
    )
}

private fun DocumentSnapshot.toMovement(groupId: String): Movement? {
    val amount = getLong(FirestoreFinanceRepository.F_AMOUNT_CENTS) ?: return null
    val epochDay = getLong(FirestoreFinanceRepository.F_DATE_EPOCH_DAY) ?: return null
    return Movement(
        id = id,
        groupId = groupId,
        type = MovementType.fromKey(getString(FirestoreFinanceRepository.F_TYPE)),
        method = PaymentMethod.fromKey(getString(FirestoreFinanceRepository.F_METHOD)),
        amountCents = amount,
        description = getString(FirestoreFinanceRepository.F_DESCRIPTION).orEmpty(),
        categoryId = getString(FirestoreFinanceRepository.F_CATEGORY_ID).orEmpty(),
        date = LocalDate.ofEpochDay(epochDay),
        createdAt = getLong(FirestoreFinanceRepository.F_CREATED_AT) ?: 0L,
        createdById = getString(FirestoreFinanceRepository.F_CREATED_BY_ID).orEmpty(),
        createdByName = getString(FirestoreFinanceRepository.F_CREATED_BY_NAME).orEmpty(),
        hasPhoto = getBoolean(FirestoreFinanceRepository.F_HAS_PHOTO) ?: false,
    )
}
