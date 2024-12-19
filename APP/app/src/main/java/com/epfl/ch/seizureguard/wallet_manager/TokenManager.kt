package com.epfl.ch.seizureguard.wallet_manager
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.epfl.ch.seizureguard.BuildConfig
import com.example.seizureguard.wallet_manager.GoogleWalletToken
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

fun loadRSAPrivateKey(privateKeyPEM: String): RSAPrivateKey {
    val privateKeyContent = privateKeyPEM
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replace("\n", "")
        .replace("\\s".toRegex(), "")

    val privateKeyBytes = Base64.getDecoder().decode(privateKeyContent)
    val keySpec = PKCS8EncodedKeySpec(privateKeyBytes)
    val keyFactory = KeyFactory.getInstance("RSA")
    return keyFactory.generatePrivate(keySpec) as RSAPrivateKey
}

fun generateToken(
    request: GoogleWalletToken.PassRequest
): String {
    val issuerId = "3388000000022811098"
    val classSuffix = "1-szg_medical_card"
    val objectSuffix = "1-szg_medical_card_${request.uid}"
    val objectId = "$issuerId.$objectSuffix"

    val genericObject = mapOf(
        "id" to objectId,
        "classId" to "$issuerId.$classSuffix",
        "logo" to mapOf(
            "sourceUri" to mapOf("uri" to "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRILcpp0CnF7ms6F5rd-PDiAK6c5aKVuWVAwQ&s"),
            "contentDescription" to mapOf(
                "defaultValue" to mapOf(
                    "language" to "en-US",
                    "value" to "LOGO_IMAGE_DESCRIPTION"
                )
            )
        ),
        "cardTitle" to mapOf(
            "defaultValue" to mapOf(
                "language" to "en-US",
                "value" to "Medical Card"
            )
        ),
        "subheader" to mapOf(
            "defaultValue" to mapOf(
                "language" to "en-US",
                "value" to "Patient"
            )
        ),
        "header" to mapOf(
            "defaultValue" to mapOf(
                "language" to "en-US",
                "value" to request.patientName
            )
        ),
        "textModulesData" to listOf(
            mapOf("id" to "emergency_contact", "header" to "Emergency Contact", "body" to request.emergencyContact),
            mapOf("id" to "birthdate", "header" to "Birthdate", "body" to request.birthdate),
            mapOf("id" to "seizure_type", "header" to "Seizure Type", "body" to request.seizureType),
            mapOf("id" to "medication", "header" to "Medication", "body" to request.medication)
        ),
        "barcode" to mapOf(
            "type" to "QR_CODE",
            "value" to "ISSUER_ID.OBJECT_ID",
            "alternateText" to ""
        ),
        "hexBackgroundColor" to "#05b3b4",
    )

    val privateKey = BuildConfig.PRIVATE_KEY
    val rsaPrivateKey = loadRSAPrivateKey(privateKey)

    val algorithm = Algorithm.RSA256(null, rsaPrivateKey)
    return JWT.create()
        .withClaim("iss", "seizureguard-wallet@seizureguard.iam.gserviceaccount.com")
        .withClaim("aud", "google")
        .withClaim("typ", "savetowallet")
        .withClaim("payload", mapOf("genericObjects" to listOf(genericObject)))
        .sign(algorithm)

}
