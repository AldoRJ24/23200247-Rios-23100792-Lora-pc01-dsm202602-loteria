package com.example.loteriaaaa.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Repositorio de Autenticación utilizando Firebase Authentication con manejo seguro anti-crashes.
 */
class AuthRepository {

    private val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }

    /**
     * Retorna el usuario actualmente autenticado en Firebase Auth.
     */
    fun getCurrentUser(): FirebaseUser? {
        return try {
            auth?.currentUser
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Retorna el correo del usuario actualmente autenticado.
     */
    fun getCurrentUserEmail(): String? {
        return getCurrentUser()?.email
    }

    /**
     * Retorna el UID del usuario actualmente autenticado.
     */
    fun getCurrentUserId(): String? {
        return getCurrentUser()?.uid
    }

    /**
     * Verifica si hay una sesión activa.
     */
    fun isUserLoggedIn(): Boolean {
        return getCurrentUser() != null
    }

    /**
     * Inicia sesión con correo electrónico y contraseña.
     */
    suspend fun loginWithEmail(email: String, pass: String): Result<FirebaseUser> {
        val firebaseAuth = auth
            ?: return Result.failure(Exception("Firebase Authentication no está inicializado o configurado."))

        return try {
            if (email.isBlank() || pass.isBlank()) {
                return Result.failure(IllegalArgumentException("El correo y la contraseña no pueden estar vacíos."))
            }
            val result = firebaseAuth.signInWithEmailAndPassword(email.trim(), pass.trim()).await()
            val user = result.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("No se pudo obtener la información del usuario."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registra un nuevo usuario con correo electrónico y contraseña.
     */
    suspend fun registerWithEmail(email: String, pass: String): Result<FirebaseUser> {
        val firebaseAuth = auth
            ?: return Result.failure(Exception("Firebase Authentication no está inicializado o configurado."))

        return try {
            if (email.isBlank() || pass.isBlank()) {
                return Result.failure(IllegalArgumentException("El correo y la contraseña no pueden estar vacíos."))
            }
            if (pass.length < 6) {
                return Result.failure(IllegalArgumentException("La contraseña debe tener al menos 6 caracteres."))
            }
            val result = firebaseAuth.createUserWithEmailAndPassword(email.trim(), pass.trim()).await()
            val user = result.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Error al registrar el usuario."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cierra la sesión activa en Firebase Auth.
     */
    fun logout() {
        try {
            auth?.signOut()
        } catch (_: Exception) {
        }
    }
}
