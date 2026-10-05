package com.proyecto.chambaya.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * FASE 5 — Modelos de publicaciones de trabajo.
 *
 * Colección: publications/{publicationId}
 * Ver CHAMBAYA_IMPLEMENTACION_FASES.md § FASE 5.
 */

object PublicationStatus {
    const val ACTIVE = "ACTIVE"
    const val PAUSED = "PAUSED"
    const val FINISHED = "FINISHED"
    const val ARCHIVED = "ARCHIVED"
    val ALL = listOf(ACTIVE, PAUSED, FINISHED, ARCHIVED)
    val EDITABLE = listOf(ACTIVE, PAUSED)
}

object PublicationVisibility {
    const val PUBLIC = "PUBLIC"
    const val HIDDEN = "HIDDEN"
}

object PublicationType {
    const val JOB_OFFER = "JOB_OFFER"
}

object PaymentPeriod {
    const val DAY = "DAY"
    const val HOUR = "HOUR"
    const val JOB = "JOB"
    const val WEEK = "WEEK"
    const val MONTH = "MONTH"

    fun label(context: android.content.Context, period: String): String = when (period) {
        HOUR -> context.getString(com.proyecto.chambaya.R.string.k_per_hora)
        DAY -> context.getString(com.proyecto.chambaya.R.string.k_per_dia)
        WEEK -> context.getString(com.proyecto.chambaya.R.string.k_per_semana)
        MONTH -> context.getString(com.proyecto.chambaya.R.string.k_per_mes)
        JOB -> context.getString(com.proyecto.chambaya.R.string.k_per_trabajo)
        else -> context.getString(com.proyecto.chambaya.R.string.k_per_dia)
    }
}

data class PaymentBlock(
    val amount: Double = 0.0,
    val currency: String = "PEN",
    val period: String = PaymentPeriod.DAY,
    val negotiable: Boolean = false
)

data class ScheduleBlock(
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val startTime: String = "",
    val endTime: String = ""
)

data class PublicationLocation(
    val district: String = "",
    val province: String = "Huamanga",
    val department: String = "Ayacucho",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val exactAddress: String = ""
)

data class PublicationImage(
    val url: String = "",
    val publicId: String = ""
)

data class PublisherBlock(
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val photoUrl: String = "",
    val verified: Boolean = false,
    val employerType: String = "",
    val sector: String = ""
)

data class PublicationStats(
    val views: Long = 0,
    val likes: Long = 0,
    val comments: Long = 0,
    val shares: Long = 0,
    val saves: Long = 0,
    val applications: Long = 0
)

data class Publication(
    val publicationId: String = "",
    val ownerUid: String = "",
    val status: String = PublicationStatus.ACTIVE,
    val visibility: String = PublicationVisibility.PUBLIC,
    val type: String = PublicationType.JOB_OFFER,
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val subcategory: String = "",
    val skillsRequired: List<String> = emptyList(),
    val payment: PaymentBlock = PaymentBlock(),
    val schedule: ScheduleBlock = ScheduleBlock(),
    val workersNeeded: Int = 1,
    val requiresExperience: Boolean = false,
    val workersHired: Int = 0,
    val location: PublicationLocation = PublicationLocation(),
    val workplaceId: String = "",
    val workplaceName: String = "",
    val images: List<PublicationImage> = emptyList(),
    val publisher: PublisherBlock = PublisherBlock(),
    val statistics: PublicationStats = PublicationStats(),
    val featured: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
)

/** Borrador del formulario de publicación (validado antes de guardar). */
data class PublicationDraft(
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val skillsRequired: List<String> = emptyList(),
    val amount: Double = 0.0,
    val period: String = PaymentPeriod.DAY,
    val negotiable: Boolean = false,
    val workersNeeded: Int = 1,
    val requiresExperience: Boolean = false,
    val district: String = "",
    val exactAddress: String = "",
    val startTime: String = "",
    val endTime: String = ""
)

object PublicationLimits {
    const val TITLE_MIN = 8
    const val TITLE_MAX = 100
    const val DESC_MIN = 20
    const val DESC_MAX = 2000
    const val SKILLS_MAX = 8
    const val WORKERS_MIN = 1
    const val WORKERS_MAX = 50
    const val IMAGES_MAX = 3
    const val AMOUNT_MAX = 100000.0
}

/** Errores de validación. Vacío = válido. */
fun validatePublicationDraft(context: android.content.Context, draft: PublicationDraft): List<String> {
    val errores = mutableListOf<String>()
    val title = draft.title.trim()
    if (title.length < PublicationLimits.TITLE_MIN) {
        errores += context.getString(com.proyecto.chambaya.R.string.kv_pub_titulo_min_fmt, PublicationLimits.TITLE_MIN)
    }
    if (title.length > PublicationLimits.TITLE_MAX) {
        errores += context.getString(com.proyecto.chambaya.R.string.kv_pub_titulo_max_fmt, PublicationLimits.TITLE_MAX)
    }
    val desc = draft.description.trim()
    if (desc.length < PublicationLimits.DESC_MIN) {
        errores += context.getString(com.proyecto.chambaya.R.string.kv_pub_desc_min_fmt, PublicationLimits.DESC_MIN)
    }
    if (desc.length > PublicationLimits.DESC_MAX) {
        errores += context.getString(com.proyecto.chambaya.R.string.kv_pub_desc_max_fmt, PublicationLimits.DESC_MAX)
    }
    if (draft.category.isBlank()) errores += context.getString(com.proyecto.chambaya.R.string.kv_pub_cat)
    if (draft.amount <= 0) errores += context.getString(com.proyecto.chambaya.R.string.kv_pub_pago)
    if (draft.amount > PublicationLimits.AMOUNT_MAX) errores += context.getString(com.proyecto.chambaya.R.string.kv_pub_monto)
    if (draft.workersNeeded !in PublicationLimits.WORKERS_MIN..PublicationLimits.WORKERS_MAX) {
        errores += context.getString(com.proyecto.chambaya.R.string.kv_pub_vacantes_fmt, PublicationLimits.WORKERS_MAX)
    }
    if (draft.district.isBlank()) errores += context.getString(com.proyecto.chambaya.R.string.kv_pub_distrito)
    if (draft.skillsRequired.size > PublicationLimits.SKILLS_MAX) {
        errores += context.getString(com.proyecto.chambaya.R.string.kv_pub_habs_fmt, PublicationLimits.SKILLS_MAX)
    }
    return errores
}

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.toPublication(): Publication {
    fun map(key: String): Map<*, *>? = get(key) as? Map<*, *>
    val payMap = map("payment")
    val schMap = map("schedule")
    val locMap = map("location")
    val pubMap = map("publisher")
    val statMap = map("statistics")
    val imgList = (get("images") as? List<*>)?.mapNotNull { item ->
        val m = item as? Map<*, *> ?: return@mapNotNull null
        val url = (m["url"] as? String).orEmpty()
        if (url.isBlank()) null else PublicationImage(url, (m["publicId"] as? String).orEmpty())
    }.orEmpty()
    return Publication(
        publicationId = getString("publicationId") ?: id,
        ownerUid = getString("ownerUid").orEmpty(),
        status = getString("status") ?: PublicationStatus.ACTIVE,
        visibility = getString("visibility") ?: PublicationVisibility.PUBLIC,
        type = getString("type") ?: PublicationType.JOB_OFFER,
        title = getString("title").orEmpty(),
        description = getString("description").orEmpty(),
        category = getString("category").orEmpty(),
        subcategory = getString("subcategory").orEmpty(),
        skillsRequired = (get("skillsRequired") as? List<*>)?.mapNotNull { it?.toString() }.orEmpty(),
        payment = PaymentBlock(
            amount = (payMap?.get("amount") as? Number)?.toDouble() ?: 0.0,
            currency = (payMap?.get("currency") as? String) ?: "PEN",
            period = (payMap?.get("period") as? String) ?: PaymentPeriod.DAY,
            negotiable = (payMap?.get("negotiable") as? Boolean) ?: false
        ),
        schedule = ScheduleBlock(
            startDate = schMap?.get("startDate") as? Timestamp,
            endDate = schMap?.get("endDate") as? Timestamp,
            startTime = (schMap?.get("startTime") as? String).orEmpty(),
            endTime = (schMap?.get("endTime") as? String).orEmpty()
        ),
        workersNeeded = (get("workersNeeded") as? Number)?.toInt() ?: 1,
        requiresExperience = (get("requiresExperience") as? Boolean) ?: false,
        workersHired = (get("workersHired") as? Number)?.toInt() ?: 0,
        location = PublicationLocation(
            district = (locMap?.get("district") as? String).orEmpty(),
            province = (locMap?.get("province") as? String) ?: "Huamanga",
            department = (locMap?.get("department") as? String) ?: "Ayacucho",
            latitude = (locMap?.get("latitude") as? Number)?.toDouble(),
            longitude = (locMap?.get("longitude") as? Number)?.toDouble(),
            exactAddress = (locMap?.get("exactAddress") as? String).orEmpty()
        ),
        workplaceId = getString("workplaceId").orEmpty(),
        workplaceName = getString("workplaceName").orEmpty(),
        images = imgList,
        publisher = PublisherBlock(
            uid = (pubMap?.get("uid") as? String).orEmpty(),
            name = (pubMap?.get("name") as? String).orEmpty(),
            username = (pubMap?.get("username") as? String).orEmpty(),
            photoUrl = (pubMap?.get("photoUrl") as? String).orEmpty(),
            verified = (pubMap?.get("verified") as? Boolean) ?: false,
            employerType = (pubMap?.get("employerType") as? String).orEmpty(),
            sector = (pubMap?.get("sector") as? String).orEmpty()
        ),
        statistics = PublicationStats(
            views = (statMap?.get("views") as? Number)?.toLong() ?: 0,
            likes = (statMap?.get("likes") as? Number)?.toLong() ?: 0,
            comments = (statMap?.get("comments") as? Number)?.toLong() ?: 0,
            shares = (statMap?.get("shares") as? Number)?.toLong() ?: 0,
            saves = (statMap?.get("saves") as? Number)?.toLong() ?: 0,
            applications = (statMap?.get("applications") as? Number)?.toLong() ?: 0
        ),
        featured = getBoolean("featured") ?: false,
        createdAt = getTimestamp("createdAt"),
        updatedAt = getTimestamp("updatedAt"),
        expiresAt = getTimestamp("expiresAt")
    )
}

/** "Hace 2h", "Ayer", "12/03/2025". */
fun publicationTimeAgo(ts: Timestamp?): String {
    if (ts == null) return "Recién publicado"
    val diff = System.currentTimeMillis() - ts.toDate().time
    if (diff < 0) return "Recién publicado"
    val min = diff / 60_000
    if (min < 1) return "Ahora mismo"
    if (min < 60) return "Hace ${min}min"
    val h = min / 60
    if (h < 24) return "Hace ${h}h"
    val d = h / 24
    if (d == 1L) return "Ayer"
    if (d < 7) return "Hace ${d}d"
    return android.text.format.DateFormat.format("dd/MM/yyyy", Date(ts.seconds * 1000)).toString()
}

/** "S/ 80 / día" o "S/ 80 · A tratar". */
fun Publication.precioTexto(context: android.content.Context): String {
    val entero = if (payment.amount % 1.0 == 0.0) payment.amount.toInt().toString()
    else String.format("%.2f", payment.amount)
    val base = "S/ $entero / ${PaymentPeriod.label(context, payment.period)}"
    return if (payment.negotiable) "$base · ${context.getString(com.proyecto.chambaya.R.string.k_precio_tratar)}" else base
}
