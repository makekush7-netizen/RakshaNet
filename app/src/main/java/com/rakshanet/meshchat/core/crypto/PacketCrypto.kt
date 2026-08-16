package com.rakshanet.meshchat.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.rakshanet.meshchat.core.protocol.CanonicalPacketCodec
import com.rakshanet.meshchat.core.protocol.MeshPacket
import com.rakshanet.meshchat.core.protocol.PacketBody
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

interface PacketSigner {
    val encodedPublicKey: ByteArray
    fun sign(bytes: ByteArray): ByteArray
}

class EphemeralPacketSigner private constructor(private val keyPair: KeyPair) : PacketSigner {
    override val encodedPublicKey: ByteArray get() = keyPair.public.encoded

    override fun sign(bytes: ByteArray): ByteArray = Signature.getInstance("SHA256withECDSA").run {
        initSign(keyPair.private)
        update(bytes)
        sign()
    }

    companion object {
        fun create(): EphemeralPacketSigner = EphemeralPacketSigner(
            KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair(),
        )
    }
}

class AndroidKeystorePacketSigner(private val alias: String = "rakshanet_packet_signing_v1") : PacketSigner {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private val keyPair: KeyPair by lazy {
        val existing = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
        if (existing != null) {
            KeyPair(existing.certificate.publicKey, existing.privateKey)
        } else {
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
                initialize(
                    KeyGenParameterSpec.Builder(
                        alias,
                        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
                    )
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
                        .build(),
                )
            }.generateKeyPair()
        }
    }

    override val encodedPublicKey: ByteArray get() = keyPair.public.encoded

    override fun sign(bytes: ByteArray): ByteArray = Signature.getInstance("SHA256withECDSA").run {
        initSign(keyPair.private)
        update(bytes)
        sign()
    }
}

object PacketAuthenticator {
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun senderId(publicKey: ByteArray): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(publicKey)
        .take(16)
        .joinToString("") { "%02x".format(it) }

    fun create(body: PacketBody, remainingTtl: Int, signer: PacketSigner): MeshPacket {
        require(body.senderId == senderId(signer.encodedPublicKey)) { "sender id does not match signing key" }
        val signature = signer.sign(CanonicalPacketCodec.signedBytes(body))
        return MeshPacket(body, remainingTtl, encoder.encodeToString(signer.encodedPublicKey), encoder.encodeToString(signature))
    }

    fun verify(packet: MeshPacket): Boolean = runCatching {
        val publicKeyBytes = decoder.decode(packet.publicKeyBase64)
        if (packet.body.senderId != senderId(publicKeyBytes)) return false
        val publicKey: PublicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(publicKeyBytes))
        Signature.getInstance("SHA256withECDSA").run {
            initVerify(publicKey)
            update(CanonicalPacketCodec.signedBytes(packet.body))
            verify(decoder.decode(packet.signatureBase64))
        }
    }.getOrDefault(false)
}
