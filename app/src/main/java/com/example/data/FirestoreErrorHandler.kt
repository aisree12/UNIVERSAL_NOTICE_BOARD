package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

fun Throwable.toFriendlyFirestoreMessage(fallback: String): String {
    val firestoreException = generateSequence(this) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    return when (firestoreException?.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Permission denied. Verify your institution membership, role, or passcode."
        FirebaseFirestoreException.Code.NOT_FOUND ->
            "Record not found. Please check the Institution ID or item."
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Network unavailable. Viewing cached content where available."
        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
            "Quota exceeded. Please try again later."
        else -> firestoreException?.localizedMessage ?: localizedMessage ?: fallback
    }
}
