package com.example.model.firestore

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Data class représentant un document utilisateur dans la collection Firestore 'users'.
 * Contient le rôle de l'utilisateur ('user' par défaut, 'admin' si configuré dans la console Firebase).
 */
data class UserDocument(
    @DocumentId
    val uid: String = "",

    @get:PropertyName("email")
    @set:PropertyName("email")
    var email: String = "",

    @get:PropertyName("role")
    @set:PropertyName("role")
    var role: String = ROLE_USER,

    @get:PropertyName("created_at")
    @set:PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis()
) {
    val isAdmin: Boolean
        get() = role == ROLE_ADMIN

    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ADMIN = "admin"
    }
}
