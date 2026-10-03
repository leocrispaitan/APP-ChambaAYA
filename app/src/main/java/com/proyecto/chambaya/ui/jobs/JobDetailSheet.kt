package com.proyecto.chambaya.ui.jobs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.ApplicationStatus
import com.proyecto.chambaya.data.model.JobStatus
import com.proyecto.chambaya.data.model.Publication
import com.proyecto.chambaya.data.model.PublicationStatus
import com.proyecto.chambaya.data.model.UserRoles
import com.proyecto.chambaya.data.model.precioTexto
import com.proyecto.chambaya.data.model.publicationTimeAgo
import com.proyecto.chambaya.data.repository.ApplicationRepository
import com.proyecto.chambaya.data.repository.CommentRepository
import com.proyecto.chambaya.data.repository.JobRepository
import com.proyecto.chambaya.data.repository.ProfileRepository
import com.proyecto.chambaya.data.repository.PublicationInteractionRepository
import com.proyecto.chambaya.data.repository.PublicationRepository
import com.proyecto.chambaya.ui.profile.ProfileCache
import kotlinx.coroutines.launch

/**
 * FASE 6-10 — Vista detallada de la chamba (BottomSheet).
 *
 * Muestra la publicación completa con jerarquía profesional y acciones según
 * el rol:
 *  - trabajador → Postularme / estado de postulación, Guardar, Compartir,
 *    Comentar
 *  - dueño contratante → Pausar / Reactivar / Finalizar
 */
class JobDetailSheet : BottomSheetDialogFragment() {

    private val pubRepo = PublicationRepository()
    private val interRepo = PublicationInteractionRepository()
    private val appRepo = ApplicationRepository()
    private val jobRepo = JobRepository()
    private val commentRepo = CommentRepository()

    private var currentPub: Publication? = null
    private var commentListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var photoPageCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottom_sheet_job_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val publicationId = requireArguments().getString(ARG_ID).orEmpty()
        if (publicationId.isBlank()) { dismiss(); return }

        // Tras postular/retirar se repinta el botón con el estado real.
        parentFragmentManager.setFragmentResultListener(ApplySheet.REQUEST_APPLIED, viewLifecycleOwner) { _, b ->
            if (b.getString(ApplySheet.EXTRA_ID) == publicationId) {
                view.findViewById<MaterialButton>(R.id.btnDetailApply)?.let { pintarBotonPostular(it) }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val pub = pubRepo.getById(publicationId).getOrNull()
            if (pub == null) {
                Toast.makeText(requireContext(), "La publicación ya no está disponible.", Toast.LENGTH_SHORT).show()
                dismiss()
                return@launch
            }
            pintar(view, publicationId)
            pubRepo.registerView(publicationId)
        }
    }

    private suspend fun pintar(view: View, publicationId: String) {
        val pub = pubRepo.getById(publicationId).getOrNull() ?: return
        currentPub = pub
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val isOwner = uid.isNotBlank() && pub.ownerUid == uid
        var saved = if (uid.isBlank()) false else interRepo.isSaved(publicationId, uid)

        // ── Publicador ──
        view.findViewById<TextView>(R.id.tvDetailName).text =
            pub.publisher.name.ifBlank { "Contratante ChambAYA" }
        val username = pub.publisher.username.ifBlank { "chambaya" }
        view.findViewById<TextView>(R.id.tvDetailMeta).text =
            "@$username · ${publicationTimeAgo(pub.createdAt)}"
        view.findViewById<ImageView>(R.id.ivDetailVerified).visibility =
            if (pub.publisher.verified) View.VISIBLE else View.GONE
        val ivAvatar = view.findViewById<ImageView>(R.id.ivDetailAvatar)
        if (pub.publisher.photoUrl.isNotBlank()) {
            ivAvatar.load(pub.publisher.photoUrl) {
                crossfade(true); placeholder(R.drawable.ic_user_circle); error(R.drawable.ic_user_circle)
            }
        }
        view.findViewById<View>(R.id.rowPublisher).setOnClickListener {
            dismiss()
            parentFragmentManager.setFragmentResult(
                REQUEST_OPEN_PROFILE,
                bundleOf(EXTRA_UID to pub.publisher.uid.ifBlank { pub.ownerUid })
            )
        }

        // ── Contenido ──
        view.findViewById<TextView>(R.id.tvDetailTitle).text = pub.title
        view.findViewById<TextView>(R.id.tvDetailCategory).text = pub.category.ifBlank { "Chamba" }
        val libres = (pub.workersNeeded - pub.workersHired).coerceAtLeast(0)
        view.findViewById<TextView>(R.id.tvDetailStatus).text =
            if (libres <= 0) "Vacantes cubiertas" else "$libres vacante${if (libres == 1) "" else "s"}"

        val urls = pub.images.mapNotNull { it.url.takeIf { u -> u.isNotBlank() } }
        montarCarruselFotos(view, urls)

        view.findViewById<TextView>(R.id.tvDetailPrice).text = pub.precioTexto()
        view.findViewById<TextView>(R.id.tvDetailNegotiable).visibility =
            if (pub.payment.negotiable) View.VISIBLE else View.GONE
        view.findViewById<TextView>(R.id.tvDetailDistrict).text =
            pub.location.district.ifBlank { "Ayacucho" }

        val horario = listOf(pub.schedule.startTime, pub.schedule.endTime)
            .filter { it.isNotBlank() }.joinToString(" – ")
        view.findViewById<TextView>(R.id.tvDetailSchedule).text = horario.ifBlank { "A convenir" }
        view.findViewById<TextView>(R.id.tvDetailWorkers).text =
            "${pub.workersNeeded} persona${if (pub.workersNeeded == 1) "" else "s"}"
        view.findViewById<TextView>(R.id.tvDetailDate).text = publicationTimeAgo(pub.createdAt)
        val wp = pub.workplaceName.ifBlank { "Por definir" }
        view.findViewById<TextView>(R.id.tvDetailWorkplace).text = wp

        view.findViewById<TextView>(R.id.tvDetailDescription).text = pub.description

        val chips = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipsSkills)
        chips.removeAllViews()
        pub.skillsRequired.take(8).forEach { skill ->
            val chip = Chip(requireContext()).apply { text = skill; isClickable = false }
            chips.addView(chip)
        }
        chips.visibility = if (pub.skillsRequired.isEmpty()) View.GONE else View.VISIBLE

        fun statsText(likes: Long, saves: Long) =
            "${formatCount(likes)} me gusta · ${formatCount(saves)} guardados · ${formatCount(pub.statistics.views + 1)} vistas"
        view.findViewById<TextView>(R.id.tvDetailStats).text = statsText(pub.statistics.likes, pub.statistics.saves)

        // ── Acciones por rol ──
        val workerActions = view.findViewById<LinearLayout>(R.id.layoutWorkerActions)
        val ownerActions = view.findViewById<LinearLayout>(R.id.layoutOwnerActions)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnDetailSave)
        val btnApply = view.findViewById<MaterialButton>(R.id.btnDetailApply)

        if (isOwner) {
            workerActions.visibility = View.GONE
            ownerActions.visibility = View.VISIBLE
            val btnPause = view.findViewById<MaterialButton>(R.id.btnOwnerPause)
            btnPause.text = if (pub.status == "PAUSED") "Reactivar" else "Pausar"
            btnPause.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    val nuevo = if (pub.status == "PAUSED") "ACTIVE" else "PAUSED"
                    val r = pubRepo.changeStatus(uid, publicationId, nuevo)
                    if (r.isSuccess) {
                        Toast.makeText(requireContext(), if (nuevo == "PAUSED") "Publicación pausada." else "Publicación reactivada.", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.setFragmentResult(REQUEST_CHANGED, bundleOf())
                        dismiss()
                    } else {
                        Toast.makeText(requireContext(), r.exceptionOrNull()?.message ?: "No se pudo actualizar.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            view.findViewById<MaterialButton>(R.id.btnOwnerFinish).setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    val r = pubRepo.changeStatus(uid, publicationId, "FINISHED")
                    if (r.isSuccess) {
                        Toast.makeText(requireContext(), "Publicación finalizada.", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.setFragmentResult(REQUEST_CHANGED, bundleOf())
                        dismiss()
                    } else {
                        Toast.makeText(requireContext(), r.exceptionOrNull()?.message ?: "No se pudo finalizar.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            workerActions.visibility = View.VISIBLE
            ownerActions.visibility = View.GONE

            fun paintSave() {
                btnSave.icon = androidx.core.content.ContextCompat.getDrawable(
                    requireContext(),
                    if (saved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
                )
            }
            paintSave()
            btnSave.setOnClickListener {
                if (uid.isBlank()) {
                    Toast.makeText(requireContext(), "Inicia sesión para guardar.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val r = interRepo.toggleSave(publicationId, uid)
                    if (r.isSuccess) {
                        saved = r.getOrDefault(false)
                        paintSave()
                        Toast.makeText(requireContext(), if (saved) "Guardado en tu lista." else "Quitado de guardados.", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.setFragmentResult(REQUEST_CHANGED, bundleOf())
                    }
                }
            }
            view.findViewById<MaterialButton>(R.id.btnDetailShare).setOnClickListener {
                compartir(pub.title, pub.description, pub.precioTexto(), pub.location.district)
                viewLifecycleOwner.lifecycleScope.launch { pubRepo.registerShare(publicationId) }
            }
            val esTrabajador = ProfileCache.perfil?.roles?.contains("TRABAJADOR") != false
            btnApply.isEnabled = esTrabajador
            btnApply.alpha = if (esTrabajador) 1f else 0.5f
            pintarBotonPostular(btnApply)
        }

        configurarComentarios(view, publicationId, uid)
    }

    /** FASE 7 — El botón refleja el estado real de mi postulación. */
    private fun pintarBotonPostular(btnApply: MaterialButton) {
        val pub = currentPub ?: return
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val esTrabajador = ProfileCache.perfil?.roles?.contains(UserRoles.TRABAJADOR) ?: true
        if (uid.isBlank() || !esTrabajador) {
            btnApply.isEnabled = false
            btnApply.alpha = 0.5f
            btnApply.text = "Postularme"
            btnApply.setOnClickListener {
                Toast.makeText(requireContext(), "Activa el modo trabajador para postularte.", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (pub.status != PublicationStatus.ACTIVE) {
            btnApply.isEnabled = false
            btnApply.alpha = 0.5f
            btnApply.text = "No disponible"
            btnApply.setOnClickListener { }
            return
        }
        btnApply.isEnabled = false
        btnApply.alpha = 0.7f
        btnApply.text = "Cargando…"
        btnApply.setOnClickListener { }
        viewLifecycleOwner.lifecycleScope.launch {
            val last = appRepo.lastFor(pub.publicationId, uid).getOrNull()
            val job = jobRepo.findByPublicationAndWorker(pub.publicationId, uid).getOrNull()
            if (!isAdded) return@launch
            when {
                last == null || ApplicationStatus.isFinal(last.status) -> {
                    btnApply.isEnabled = true
                    btnApply.alpha = 1f
                    btnApply.text = "Postularme"
                    btnApply.setOnClickListener {
                        ApplySheet.newInstance(pub.publicationId).show(parentFragmentManager, "apply")
                    }
                }
                last.status == ApplicationStatus.PENDING -> {
                    btnApply.isEnabled = true
                    btnApply.alpha = 1f
                    btnApply.text = "Postulación enviada"
                    btnApply.setOnClickListener {
                        ApplySheet.newInstance(pub.publicationId).show(parentFragmentManager, "apply")
                    }
                }
                last.status == ApplicationStatus.ACCEPTED -> {
                    btnApply.isEnabled = false
                    btnApply.alpha = 1f
                    btnApply.text = when (job?.status) {
                        JobStatus.IN_PROGRESS -> "Trabajo en curso"
                        JobStatus.COMPLETED -> "Trabajo completado ✓"
                        else -> "¡Fuiste seleccionado!"
                    }
                    btnApply.setOnClickListener { }
                }
                else -> {
                    btnApply.isEnabled = true
                    btnApply.alpha = 1f
                    btnApply.text = "Postularme"
                    btnApply.setOnClickListener {
                        ApplySheet.newInstance(pub.publicationId).show(parentFragmentManager, "apply")
                    }
                }
            }
        }
    }

    /** Carrusel de fotos: 0 = oculto, 1 = foto fija sin dots, 2+ = swipe + dots. */
    private fun montarCarruselFotos(view: View, urls: List<String>) {
        val card = view.findViewById<MaterialCardView>(R.id.cardDetailPhoto)
        val vp = view.findViewById<ViewPager2>(R.id.vpDetailPhotos)
        val dots = view.findViewById<LinearLayout>(R.id.dotsDetailPhotos)
        photoPageCallback?.let { vp.unregisterOnPageChangeCallback(it) }
        photoPageCallback = null
        if (urls.isEmpty()) {
            card.visibility = View.GONE
            dots.visibility = View.GONE
            return
        }
        card.visibility = View.VISIBLE
        vp.adapter = DetailPhotoAdapter(urls)
        if (urls.size == 1) {
            // Una sola foto: sin dots y sin swipe.
            dots.visibility = View.GONE
            vp.isUserInputEnabled = false
            return
        }
        vp.isUserInputEnabled = true
        dots.visibility = View.VISIBLE
        // Se pinta tras el layout: si se construyen los dots en el mismo
        // frame del GONE→VISIBLE, el NestedScrollView puede medir el
        // contenedor con altura 0 y solo aparece al repintar (al deslizar).
        dots.post {
            if (!isAdded) return@post
            pintarDotsFotos(dots, urls.size, vp.currentItem)
            dots.requestLayout()
        }
        val callback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                pintarDotsFotos(dots, urls.size, position)
            }
        }
        photoPageCallback = callback
        vp.registerOnPageChangeCallback(callback)
    }

    /** Dots propios del detalle (no usa los de bienvenida ni los del home). */
    private fun pintarDotsFotos(dots: LinearLayout, total: Int, activo: Int) {
        val density = dots.resources.displayMetrics.density
        dots.removeAllViews()
        repeat(total) { i ->
            val dot = View(requireContext()).apply {
                val size = (8 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    if (i > 0) marginStart = (5 * density).toInt()
                }
                setBackgroundResource(
                    if (i == activo) R.drawable.bg_dot_detail_active
                    else R.drawable.bg_dot_detail_inactive
                )
            }
            dots.addView(dot)
        }
    }

    /** FASE 10 — Comentarios en vivo + publicar + menú propio/ajeno. */
    private fun configurarComentarios(view: View, publicationId: String, uid: String) {
        val rv = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvComments)
        val tvTitle = view.findViewById<TextView>(R.id.tvCommentsTitle)
        val tvEmpty = view.findViewById<TextView>(R.id.tvCommentsEmpty)
        val et = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etComment)
        val btnSend = view.findViewById<MaterialButton>(R.id.btnSendComment)
        val adapter = CommentAdapter { c, anchor -> menuComentario(c, anchor, publicationId, uid) }
        rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        rv.isNestedScrollingEnabled = false
        rv.adapter = adapter
        commentListener?.remove()
        commentListener = commentRepo.listen(
            publicationId, 50,
            onUpdate = { list ->
                if (!isAdded) return@listen
                tvTitle.text = if (list.isEmpty()) "Comentarios" else "Comentarios (${list.size})"
                tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                adapter.submitList(list)
            },
            onError = {
                if (!isAdded) return@listen
                tvEmpty.text = "No se pudieron cargar los comentarios."
            }
        )
        btnSend.setOnClickListener {
            val texto = et.text?.toString().orEmpty()
            if (uid.isBlank()) {
                Toast.makeText(requireContext(), "Inicia sesión para comentar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (texto.isBlank()) return@setOnClickListener
            btnSend.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                var perfil = ProfileCache.perfil
                if (perfil == null) {
                    perfil = ProfileRepository().loadProfile(uid).getOrNull()
                    if (perfil != null) ProfileCache.perfil = perfil
                }
                if (perfil == null) {
                    btnSend.isEnabled = true
                    return@launch
                }
                val r = commentRepo.add(uid, publicationId, perfil, texto)
                if (!isAdded) return@launch
                btnSend.isEnabled = true
                if (r.isSuccess) {
                    et.text?.clear()
                    val dueño = currentPub?.ownerUid.orEmpty()
                    if (dueño.isNotBlank() && dueño != uid) {
                        com.proyecto.chambaya.data.repository.NotificationRepository().push(
                            recipientUid = dueño,
                            type = com.proyecto.chambaya.data.model.NotificationType.NEW_COMMENT,
                            title = "Nuevo comentario",
                            message = "Comentaron tu chamba “${currentPub?.title?.take(60).orEmpty()}”.",
                            senderUid = uid,
                            publicationId = publicationId
                        )
                    }
                    parentFragmentManager.setFragmentResult(REQUEST_CHANGED, bundleOf())
                } else {
                    Toast.makeText(
                        requireContext(),
                        r.exceptionOrNull()?.message ?: "No se pudo comentar.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun menuComentario(
        c: com.proyecto.chambaya.data.model.PublicationComment,
        anchor: View,
        publicationId: String,
        uid: String
    ) {
        val menu = androidx.appcompat.widget.PopupMenu(requireContext(), anchor)
        if (c.authorUid == uid && uid.isNotBlank()) {
            menu.menu.add(0, 1, 0, "Editar")
            menu.menu.add(0, 2, 0, "Eliminar")
        } else {
            menu.menu.add(0, 3, 0, "Denunciar")
        }
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> dialogEditarComentario(c)
                2 -> androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar comentario")
                    .setMessage("Se quitará de la publicación.")
                    .setPositiveButton("Eliminar") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            val r = commentRepo.delete(uid, c)
                            if (isAdded && r.isSuccess) {
                                parentFragmentManager.setFragmentResult(REQUEST_CHANGED, bundleOf())
                            } else if (isAdded) {
                                Toast.makeText(requireContext(), "No se pudo eliminar.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
                3 -> dialogDenunciarComentario(c, publicationId, uid)
            }
            true
        }
        menu.show()
    }

    private fun dialogEditarComentario(c: com.proyecto.chambaya.data.model.PublicationComment) {
        val input = com.google.android.material.textfield.TextInputEditText(requireContext())
        input.setText(c.text)
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Editar comentario")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val r = commentRepo.edit(c.authorUid, c.commentId, input.text?.toString().orEmpty())
                    if (isAdded && r.isFailure) {
                        Toast.makeText(
                            requireContext(),
                            r.exceptionOrNull()?.message ?: "No se pudo editar.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dialogDenunciarComentario(
        c: com.proyecto.chambaya.data.model.PublicationComment,
        publicationId: String,
        uid: String
    ) {
        if (uid.isBlank()) {
            Toast.makeText(requireContext(), "Inicia sesión para denunciar.", Toast.LENGTH_SHORT).show()
            return
        }
        val motivos = arrayOf("Spam", "Contenido inapropiado", "Acoso", "Fraude", "Otro")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Denunciar comentario")
            .setItems(motivos) { _, cual ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val r = commentRepo.report(publicationId, c.commentId, uid, motivos[cual])
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            if (r.isSuccess) "Denuncia enviada. La revisaremos." else "No se pudo enviar.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        commentListener?.remove()
        commentListener = null
        photoPageCallback?.let { cb ->
            view?.findViewById<ViewPager2>(R.id.vpDetailPhotos)?.unregisterOnPageChangeCallback(cb)
        }
        photoPageCallback = null
        super.onDestroyView()
    }

    private fun compartir(titulo: String, desc: String, precio: String, distrito: String) {
        val texto = "📢 $titulo\n\n💰 $precio\n📍 $distrito\n\n${desc.take(300)}\n\n🔗 Compartido desde ChambAYA"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Chamba: $titulo")
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        startActivity(Intent.createChooser(intent, "Compartir chamba"))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState)
    }

    companion object {
        const val REQUEST_CHANGED = "job_detail_changed"
        const val REQUEST_OPEN_PROFILE = "job_detail_open_profile"
        const val EXTRA_UID = "uid"
        private const val ARG_ID = "publicationId"
        fun newInstance(publicationId: String) = JobDetailSheet().apply {
            arguments = bundleOf(ARG_ID to publicationId)
        }
    }
}
