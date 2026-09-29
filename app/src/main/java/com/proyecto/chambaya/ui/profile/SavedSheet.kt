package com.proyecto.chambaya.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.repository.PublicationInteractionRepository
import com.proyecto.chambaya.data.repository.PublicationRepository
import com.proyecto.chambaya.ui.jobs.JobDetailSheet
import com.proyecto.chambaya.ui.jobs.PublicProfileSheet
import com.proyecto.chambaya.ui.jobs.PublicationAdapter
import com.proyecto.chambaya.ui.jobs.PublicationFeedItem
import com.proyecto.chambaya.ui.jobs.PublicationOptionsSheet
import kotlinx.coroutines.launch

/**
 * FASE 11/17 — Chambas guardadas con las mismas tarjetas interactivas del feed.
 */
class SavedSheet : BottomSheetDialogFragment() {

    private val pubRepo = PublicationRepository()
    private val interRepo = PublicationInteractionRepository()
    private var adapter: PublicationAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottom_sheet_saved, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (uid.isBlank()) { dismiss(); return }

        val rv = view.findViewById<RecyclerView>(R.id.rvSaved)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = PublicationAdapter(
            onOpenDetail = { item ->
                JobDetailSheet.newInstance(item.publication.publicationId)
                    .show(parentFragmentManager, "detail")
            },
            onOpenProfile = { item ->
                val id = item.publication.publisher.uid.ifBlank { item.publication.ownerUid }
                if (id.isNotBlank()) PublicProfileSheet.newInstance(id).show(parentFragmentManager, "profile")
            },
            onToggleLike = { item -> toggleLike(uid, item) },
            onToggleSave = { item -> toggleSave(uid, item) },
            onShare = { item -> compartir(item) },
            onHide = { },
            onReport = { }
        )
        rv.adapter = adapter
        val progress = view.findViewById<ProgressBar>(R.id.progressSaved)
        val empty = view.findViewById<TextView>(R.id.tvSavedEmpty)
        progress.visibility = View.VISIBLE

        parentFragmentManager.setFragmentResultListener(PublicationOptionsSheet.REQUEST, viewLifecycleOwner) { _, b ->
            manejarOpcion(
                uid,
                b.getString(PublicationOptionsSheet.EXTRA_ACTION).orEmpty(),
                b.getString(PublicationOptionsSheet.EXTRA_ID).orEmpty()
            )
        }
        parentFragmentManager.setFragmentResultListener(JobDetailSheet.REQUEST_CHANGED, viewLifecycleOwner) { _, _ ->
            cargar(uid)
        }
        cargar(uid)
    }

    private fun cargar(uid: String) {
        val view = view ?: return
        val progress = view.findViewById<ProgressBar>(R.id.progressSaved)
        val empty = view.findViewById<TextView>(R.id.tvSavedEmpty)
        viewLifecycleOwner.lifecycleScope.launch {
            val saves = interRepo.mySaves(uid).getOrNull().orEmpty()
            val pubs = if (saves.isEmpty()) emptyList()
            else pubRepo.getByIds(saves.toList()).getOrNull().orEmpty()
            val liked = interRepo.likedIds(pubs.map { it.publicationId }, uid)
            if (!isAdded) return@launch
            progress.visibility = View.GONE
            if (pubs.isEmpty()) {
                empty.visibility = View.VISIBLE
            } else {
                empty.visibility = View.GONE
                adapter?.submitList(
                    pubs.map { p ->
                        PublicationFeedItem(
                            p,
                            liked = p.publicationId in liked,
                            saved = true,
                            likesCount = p.statistics.likes,
                            savesCount = p.statistics.saves
                        )
                    }
                )
            }
        }
    }

    private fun toggleLike(uid: String, item: PublicationFeedItem) {
        item.liked = !item.liked
        item.likesCount += if (item.liked) 1 else -1
        adapter?.notifyDataSetChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            if (interRepo.toggleLike(item.publication.publicationId, uid).isFailure) {
                item.liked = !item.liked
                item.likesCount += if (item.liked) 1 else -1
                adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun toggleSave(uid: String, item: PublicationFeedItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            val r = interRepo.toggleSave(item.publication.publicationId, uid)
            if (!isAdded) return@launch
            if (r.isSuccess && !r.getOrDefault(true)) {
                // Se quitó: desaparece de esta lista.
                cargar(uid)
                Toast.makeText(requireContext(), "Quitado de guardados.", Toast.LENGTH_SHORT).show()
            } else if (r.isFailure) {
                Toast.makeText(requireContext(), "No se pudo guardar.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun compartir(item: PublicationFeedItem) {
        val p = item.publication
        val texto = "📢 ${p.title}\n\n📍 ${p.location.district}\n\n${p.description.take(280)}\n\n🔗 Compartido desde ChambAYA"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Chamba: ${p.title}")
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        startActivity(Intent.createChooser(intent, "Compartir chamba"))
        viewLifecycleOwner.lifecycleScope.launch { pubRepo.registerShare(p.publicationId) }
    }

    private fun manejarOpcion(uid: String, action: String, publicationId: String) {
        if (publicationId.isBlank()) return
        val item = adapter?.currentList?.firstOrNull { it.publication.publicationId == publicationId } ?: return
        when (action) {
            PublicationOptionsSheet.ACTION_SAVE -> toggleSave(uid, item)
            PublicationOptionsSheet.ACTION_SHARE -> compartir(item)
            PublicationOptionsSheet.ACTION_HIDE -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    interRepo.hide(publicationId, uid)
                    if (isAdded) cargar(uid)
                }
            }
            PublicationOptionsSheet.ACTION_REPORT -> mostrarDenuncia(uid, item)
            PublicationOptionsSheet.ACTION_WHY -> AlertDialog.Builder(requireContext())
                .setTitle("Por qué ves esto")
                .setMessage("Ves esta chamba por tu ubicación (Ayacucho) y las categorías que exploras. Guárdala si te interesa o toca “No me interesa”.")
                .setPositiveButton("Entendido", null)
                .show()
            PublicationOptionsSheet.ACTION_RATE ->
                Toast.makeText(requireContext(), "Podrás calificar al completar un trabajo (Fase 9).", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDenuncia(uid: String, item: PublicationFeedItem) {
        val motivos = arrayOf("Fraude o estafa", "Contenido inapropiado", "Información falsa", "Spam", "Otro")
        AlertDialog.Builder(requireContext())
            .setTitle("Denunciar publicación")
            .setItems(motivos) { _, cual ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val r = interRepo.report(item.publication.publicationId, uid, motivos[cual])
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
        adapter = null
        super.onDestroyView()
    }
}
