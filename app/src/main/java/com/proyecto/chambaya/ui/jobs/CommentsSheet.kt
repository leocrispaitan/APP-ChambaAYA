package com.proyecto.chambaya.ui.jobs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.NotificationType
import com.proyecto.chambaya.data.model.Publication
import com.proyecto.chambaya.data.repository.CommentRepository
import com.proyecto.chambaya.data.repository.NotificationRepository
import com.proyecto.chambaya.data.repository.ProfileRepository
import com.proyecto.chambaya.data.repository.PublicationRepository
import com.proyecto.chambaya.ui.profile.ProfileCache
import kotlinx.coroutines.launch

/** Lista y formulario de comentarios sin abrir el detalle completo de la chamba. */
class CommentsSheet : BottomSheetDialogFragment() {

    private val commentRepo = CommentRepository()
    private val publicationRepo = PublicationRepository()
    private var publication: Publication? = null
    private var commentListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.bottom_sheet_comments, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val publicationId = requireArguments().getString(ARG_ID).orEmpty()
        if (publicationId.isBlank()) {
            dismiss()
            return
        }

        view.findViewById<View>(R.id.btnCommentsClose).setOnClickListener { dismiss() }
        val list = view.findViewById<RecyclerView>(R.id.rvComments)
        val empty = view.findViewById<TextView>(R.id.tvCommentsEmpty)
        val commentsAdapter = CommentAdapter { comment, anchor ->
            mostrarMenuComentario(comment, anchor, publicationId)
        }
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = commentsAdapter

        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        viewLifecycleOwner.lifecycleScope.launch {
            val pub = publicationRepo.getById(publicationId).getOrNull()
            if (!isAdded) return@launch
            if (pub == null) {
                Toast.makeText(requireContext(), "La publicación ya no está disponible.", Toast.LENGTH_SHORT).show()
                dismiss()
                return@launch
            }
            publication = pub
            commentListener = commentRepo.listen(
                publicationId,
                50,
                onUpdate = { comments ->
                    if (!isAdded) return@listen
                    view.findViewById<TextView>(R.id.tvCommentsTitle).text =
                        if (comments.isEmpty()) "Comentarios" else "Comentarios (${comments.size})"
                    empty.visibility = if (comments.isEmpty()) View.VISIBLE else View.GONE
                    commentsAdapter.submitList(comments)
                },
                onError = {
                    if (isAdded) {
                        empty.text = "No se pudieron cargar los comentarios."
                        empty.visibility = View.VISIBLE
                    }
                }
            )
        }

        view.findViewById<View>(R.id.btnSendComment).setOnClickListener { button ->
            val input = view.findViewById<TextInputEditText>(R.id.etComment)
            val text = input.text?.toString().orEmpty().trim()
            if (uid.isBlank()) {
                Toast.makeText(requireContext(), "Inicia sesión para comentar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (text.isBlank()) return@setOnClickListener

            button.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                var profile = ProfileCache.perfil
                if (profile == null) {
                    profile = ProfileRepository().loadProfile(uid).getOrNull()
                    if (profile != null) ProfileCache.perfil = profile
                }
                val currentPublication = publication
                if (profile == null || currentPublication == null) {
                    button.isEnabled = true
                    return@launch
                }
                val result = commentRepo.add(uid, publicationId, profile, text)
                if (!isAdded) return@launch
                button.isEnabled = true
                if (result.isSuccess) {
                    input.text?.clear()
                    val ownerUid = currentPublication.ownerUid
                    if (ownerUid.isNotBlank() && ownerUid != uid) {
                        NotificationRepository().push(
                            recipientUid = ownerUid,
                            type = NotificationType.NEW_COMMENT,
                            title = "Nuevo comentario",
                            message = "Comentaron tu chamba: ${currentPublication.title.take(60)}",
                            senderUid = uid,
                            publicationId = publicationId
                        )
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        result.exceptionOrNull()?.message ?: "No se pudo comentar.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun mostrarMenuComentario(
        comment: com.proyecto.chambaya.data.model.PublicationComment,
        anchor: View,
        publicationId: String
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val menu = androidx.appcompat.widget.PopupMenu(requireContext(), anchor)
        if (uid.isNotBlank() && comment.authorUid == uid) {
            menu.menu.add(0, 1, 0, "Editar")
            menu.menu.add(0, 2, 0, "Eliminar")
        } else {
            menu.menu.add(0, 3, 0, "Denunciar")
        }
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> editarComentario(comment)
                2 -> androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar comentario")
                    .setMessage("Se quitará de la publicación.")
                    .setPositiveButton("Eliminar") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            val result = commentRepo.delete(uid, comment)
                            if (isAdded && result.isFailure) {
                                Toast.makeText(requireContext(), "No se pudo eliminar.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
                3 -> denunciarComentario(comment, publicationId, uid)
            }
            true
        }
        menu.show()
    }

    private fun editarComentario(comment: com.proyecto.chambaya.data.model.PublicationComment) {
        val input = TextInputEditText(requireContext()).apply { setText(comment.text) }
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Editar comentario")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = commentRepo.edit(comment.authorUid, comment.commentId, input.text?.toString().orEmpty())
                    if (isAdded && result.isFailure) {
                        Toast.makeText(
                            requireContext(),
                            result.exceptionOrNull()?.message ?: "No se pudo editar.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun denunciarComentario(
        comment: com.proyecto.chambaya.data.model.PublicationComment,
        publicationId: String,
        uid: String
    ) {
        if (uid.isBlank()) {
            Toast.makeText(requireContext(), "Inicia sesión para denunciar.", Toast.LENGTH_SHORT).show()
            return
        }
        val reasons = arrayOf("Spam", "Contenido inapropiado", "Acoso", "Fraude", "Otro")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Denunciar comentario")
            .setItems(reasons) { _, index ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = commentRepo.report(publicationId, comment.commentId, uid, reasons[index])
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            if (result.isSuccess) "Denuncia enviada. La revisaremos." else "No se pudo enviar.",
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
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ID = "publicationId"

        fun newInstance(publicationId: String) = CommentsSheet().apply {
            arguments = bundleOf(ARG_ID to publicationId)
        }
    }
}
