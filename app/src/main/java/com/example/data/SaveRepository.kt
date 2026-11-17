package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SaveRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Debes iniciar sesión con Google antes de usar el guardado en la nube.")
    }

    fun observeSaveSlots(userId: String): Flow<List<SaveSlot>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "El usuario activo cambió." }
        val path = "users/$userId/save_slots"
        emit(emptyList())
        db.collection("users")
            .document(userId)
            .collection("save_slots")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SaveSlot::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun saveToSlot(userId: String, slotId: String, slot: SaveSlot): Result<Unit> {
        val docPath = "users/$userId/save_slots/$slotId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "La sesión activa no coincide con el propietario." }
            require(slotId.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$"))) { "ID de ranura inválido." }

            val docRef = db.collection("users")
                .document(userId)
                .collection("save_slots")
                .document(slotId)

            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(slot.toUpdateMap()).await()
            } else {
                docRef.set(slot.toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSlot(userId: String, slotId: String): Result<Unit> {
        val docPath = "users/$userId/save_slots/$slotId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "La sesión activa no coincide con el propietario." }
            require(slotId.isNotBlank()) { "ID de ranura inválido." }
            db.collection("users")
                .document(userId)
                .collection("save_slots")
                .document(slotId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
