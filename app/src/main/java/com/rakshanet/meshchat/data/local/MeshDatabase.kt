package com.rakshanet.meshchat.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rakshanet.meshchat.core.protocol.MeshPacket
import com.rakshanet.meshchat.core.protocol.PacketBody
import com.rakshanet.meshchat.core.protocol.PacketType
import com.rakshanet.meshchat.core.store.DeliveryState
import com.rakshanet.meshchat.core.store.KnownPeer
import com.rakshanet.meshchat.core.store.MeshStore
import com.rakshanet.meshchat.core.store.StoredMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "seen_packets")
data class SeenPacketEntity(@PrimaryKey val packetId: String, val seenAtMs: Long)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val packetId: String,
    val protocolVersion: Int,
    val type: String,
    val senderId: String,
    val senderName: String,
    val recipientId: String?,
    val channelId: String,
    val referencePacketId: String?,
    val payload: String,
    val timestampMs: Long,
    val originalTtl: Int,
    val remainingTtl: Int,
    val publicKeyBase64: String,
    val signatureBase64: String,
    val receivedAtMs: Long,
    val isLocal: Boolean,
    val deliveryState: String,
)

@Entity(tableName = "peers")
data class PeerEntity(
    @PrimaryKey val peerId: String,
    val displayName: String?,
    val lastSeenMs: Long,
    val observedHops: Int,
)

@Entity(tableName = "course_progress")
data class CourseProgressEntity(
    @PrimaryKey val stepId: String,
    val completedAtMs: Long,
    val score: Int?,
    val maxScore: Int?,
)

@Dao
interface MeshDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSeen(seen: SeenPacketEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPeer(peer: PeerEntity)

    @Query("SELECT * FROM messages WHERE type = 'TEXT_MESSAGE' ORDER BY receivedAtMs ASC, packetId ASC")
    fun observeMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE type IN ('SOS_ALERT', 'SOS_UPDATE', 'GUIDANCE_BROADCAST') ORDER BY receivedAtMs DESC, packetId ASC")
    fun observeAlerts(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM peers ORDER BY displayName COLLATE NOCASE ASC")
    fun observePeers(): Flow<List<PeerEntity>>

    @Query("UPDATE messages SET deliveryState = 'DELIVERED' WHERE packetId = :packetId AND isLocal = 1")
    suspend fun markDelivered(packetId: String)

    @Query("DELETE FROM messages WHERE type = 'TEXT_MESSAGE'")
    suspend fun clearMessages()
}

@Dao
interface CourseProgressDao {
    @Query("SELECT * FROM course_progress ORDER BY completedAtMs ASC")
    fun observeAll(): Flow<List<CourseProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: CourseProgressEntity)
}

@Database(
    entities = [SeenPacketEntity::class, MessageEntity::class, PeerEntity::class, CourseProgressEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class MeshDatabase : RoomDatabase() {
    abstract fun meshDao(): MeshDao
    abstract fun courseProgressDao(): CourseProgressDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE messages ADD COLUMN senderName TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE messages ADD COLUMN recipientId TEXT")
                database.execSQL("ALTER TABLE messages ADD COLUMN channelId TEXT NOT NULL DEFAULT 'community'")
                database.execSQL("ALTER TABLE messages ADD COLUMN referencePacketId TEXT")
                database.execSQL("ALTER TABLE messages ADD COLUMN deliveryState TEXT NOT NULL DEFAULT 'QUEUED'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS course_progress (" +
                        "stepId TEXT NOT NULL, completedAtMs INTEGER NOT NULL, " +
                        "score INTEGER, maxScore INTEGER, PRIMARY KEY(stepId))",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE peers ADD COLUMN observedHops INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}

class RoomMeshStore(private val database: MeshDatabase) : MeshStore {
    override val messages: Flow<List<StoredMessage>> = database.meshDao().observeMessages().map { rows -> rows.map { it.toStoredMessage() } }
    override val alerts: Flow<List<StoredMessage>> = database.meshDao().observeAlerts().map { rows -> rows.map { it.toStoredMessage() } }
    override val peers: Flow<List<KnownPeer>> = database.meshDao().observePeers().map { rows ->
        rows.map { KnownPeer(it.peerId, it.displayName.orEmpty().ifBlank { it.peerId.take(12) }, it.lastSeenMs, it.observedHops) }
    }

    override suspend fun recordIfNew(packetId: String, seenAtMs: Long, message: StoredMessage?): Boolean = database.withTransaction {
        if (database.meshDao().insertSeen(SeenPacketEntity(packetId, seenAtMs)) == -1L) return@withTransaction false
        if (message != null) database.meshDao().insertMessage(message.toEntity())
        true
    }

    override suspend fun upsertPeer(peer: KnownPeer) = database.meshDao().upsertPeer(PeerEntity(peer.peerId, peer.displayName, peer.lastSeenMs, peer.observedHops))
    override suspend fun markDelivered(packetId: String) = database.meshDao().markDelivered(packetId)
    override suspend fun clearMessages() = database.meshDao().clearMessages()
}

private fun StoredMessage.toEntity() = MessageEntity(
    packet.body.id, packet.body.protocolVersion, packet.body.type.name, packet.body.senderId,
    packet.body.senderName, packet.body.recipientId, packet.body.channelId, packet.body.referencePacketId,
    packet.body.payload, packet.body.timestampMs, packet.body.originalTtl, packet.remainingTtl,
    packet.publicKeyBase64, packet.signatureBase64, receivedAtMs, isLocal, deliveryState.name,
)

private fun MessageEntity.toStoredMessage() = StoredMessage(
    MeshPacket(
        PacketBody(protocolVersion, packetId, PacketType.valueOf(type), senderId, senderName.ifBlank { senderId.take(12) },
            recipientId, channelId, referencePacketId, payload, timestampMs, originalTtl),
        remainingTtl, publicKeyBase64, signatureBase64,
    ),
    receivedAtMs,
    isLocal,
    runCatching { DeliveryState.valueOf(deliveryState) }.getOrDefault(DeliveryState.QUEUED),
)
