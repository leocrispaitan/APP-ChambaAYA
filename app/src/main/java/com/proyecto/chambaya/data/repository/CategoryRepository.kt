package com.proyecto.chambaya.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FASE 15 — Categorías oficiales de la plataforma.
 *
 * Lectura abierta para autenticados; la escritura es administrativa
 * (seed con credenciales de proyecto, no desde la app).
 */
data class JobCategory(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val active: Boolean = true,
    val order: Int = 999
)

fun DocumentSnapshot.toJobCategory(): JobCategory {
    return JobCategory(
        id = getString("id") ?: id,
        name = getString("name").orEmpty(),
        icon = getString("icon").orEmpty(),
        active = getBoolean("active") ?: true,
        order = (get("order") as? Number)?.toInt() ?: 999
    )
}

class CategoryRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun list(): Result<List<JobCategory>> = withContext(Dispatchers.IO) {
        runCatching {
            Tasks.await(
                firestore.collection(COLLECTION)
                    .whereEqualTo("active", true)
                    .orderBy("order", Query.Direction.ASCENDING)
                    .limit(50)
                    .get()
            ).documents.map { it.toJobCategory() }.filter { it.name.isNotBlank() }
        }
    }

    companion object {
        const val COLLECTION = "categories"
    }
}
