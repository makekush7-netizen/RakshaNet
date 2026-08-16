package com.rakshanet.meshchat

import android.content.Context
import android.os.Build
import androidx.room.Room
import com.rakshanet.meshchat.core.crypto.AndroidKeystorePacketSigner
import com.rakshanet.meshchat.core.crypto.EphemeralPacketSigner
import com.rakshanet.meshchat.core.crypto.PacketAuthenticator
import com.rakshanet.meshchat.core.protocol.InboundPacket
import com.rakshanet.meshchat.core.protocol.PacketBody
import com.rakshanet.meshchat.core.protocol.PacketType
import com.rakshanet.meshchat.core.routing.MeshCoordinator
import com.rakshanet.meshchat.core.transport.MockPacketRouter
import com.rakshanet.meshchat.core.transport.NearbyPacketRouter
import com.rakshanet.meshchat.core.transport.SelectablePacketRouter
import com.rakshanet.meshchat.courses.CourseRepository
import com.rakshanet.meshchat.data.local.DeviceProfile
import com.rakshanet.meshchat.data.local.MeshDatabase
import com.rakshanet.meshchat.data.local.RoomMeshStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Process-level mesh owner. The foreground service starts/stops its real
 * transport; activities only observe and issue commands. No radio object is
 * tied to an Activity or screen lifecycle.
 */
class MeshRuntime(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val signer = AndroidKeystorePacketSigner()

    val deviceProfile = DeviceProfile(appContext, Build.MODEL.take(32))
    val mockRouter = MockPacketRouter()
    val router = SelectablePacketRouter(
        mock = mockRouter,
        nearby = NearbyPacketRouter(
            context = appContext,
            localEndpointName = deviceProfile.displayName.value,
            localIdentity = PacketAuthenticator.senderId(signer.encodedPublicKey),
        ),
    )

    private val database = Room.databaseBuilder(appContext, MeshDatabase::class.java, "rakshanet_mesh.db")
        .addMigrations(MeshDatabase.MIGRATION_1_2, MeshDatabase.MIGRATION_2_3, MeshDatabase.MIGRATION_3_4)
        .build()

    val courseRepository = CourseRepository(database.courseProgressDao())

    val coordinator = MeshCoordinator(
        router = router,
        store = RoomMeshStore(database),
        localSigner = signer,
        localDisplayName = { deviceProfile.displayName.value },
        scope = scope,
    ).also(MeshCoordinator::start)

    fun startNearby() = router.useNearby()

    fun stopNearby() = router.useMock()

    fun renameDevice(name: String) {
        if (deviceProfile.updateDisplayName(name)) router.updateNearbyName(deviceProfile.displayName.value)
    }

    fun clearChat() {
        scope.launch { coordinator.clearChat() }
    }

    fun injectDebugMessage() {
        scope.launch {
            val remoteSigner = EphemeralPacketSigner.create()
            val body = PacketBody(
                id = UUID.randomUUID().toString(),
                type = PacketType.TEXT_MESSAGE,
                senderId = PacketAuthenticator.senderId(remoteSigner.encodedPublicKey),
                senderName = "Debug peer",
                payload = "Mock incoming message — signature verified before display.",
                timestampMs = System.currentTimeMillis(),
            )
            mockRouter.inject(InboundPacket(PacketAuthenticator.create(body, body.originalTtl, remoteSigner), "debug-peer"))
        }
    }
}
