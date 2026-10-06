package com.chiiraac.migasto.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val inviteCode: String,
    val createdAt: Long,
)

@Entity(
    tableName = "movements",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("groupId"), Index("groupId", "dateEpochDay")],
)
data class MovementEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val type: String,
    val method: String,
    val amountCents: Long,
    val description: String,
    val categoryId: String,
    val dateEpochDay: Long,
    val createdAt: Long,
    val createdById: String,
    val createdByName: String,
    val hasPhoto: Boolean,
)

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun get(id: String): GroupEntity?

    @Query("SELECT * FROM groups")
    suspend fun getAll(): List<GroupEntity>

    @Upsert
    suspend fun upsert(group: GroupEntity)

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM groups")
    suspend fun deleteAll()
}

@Dao
interface MovementDao {
    @Query("SELECT * FROM movements WHERE groupId = :groupId ORDER BY dateEpochDay DESC, createdAt DESC")
    fun observeByGroup(groupId: String): Flow<List<MovementEntity>>

    @Query("SELECT * FROM movements WHERE id = :id")
    suspend fun get(id: String): MovementEntity?

    @Query("SELECT id FROM movements WHERE groupId = :groupId AND hasPhoto = 1")
    suspend fun photoIdsInGroup(groupId: String): List<String>

    @Upsert
    suspend fun upsert(movement: MovementEntity)

    @Query("DELETE FROM movements WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE movements SET createdByName = :name WHERE createdById = :uid")
    suspend fun renameAuthor(uid: String, name: String)
}

@Database(
    entities = [GroupEntity::class, MovementEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun movementDao(): MovementDao

    companion object {
        fun create(context: Context): LocalDatabase =
            Room.databaseBuilder(context, LocalDatabase::class.java, "migasto.db").build()
    }
}
