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
                Toast.makeText(requireContext(), getString(R.string.k_com_no_disponible), Toast.LENGTH_SHORT).show()
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
                        if (comments.isEmpty()) getString(R.string.sheet_comentarios_titulo) else getString(R.string.k_comentarios_conteo, comments.size)
                    empty.visibility = if (comments.isEmpty()) View.VISIBLE else View.GONE
                    commentsAdapter.submitList(comments)
                },
                onError = {
                    if (isAdded) {
                        empty.text = getString(R.string.k_com_cargar_error)
                        empty.visibility = View.VISIBLE
                    }
                }
            )
        }

        view.findViewById<View>(R.id.btnSendComment).setOnClickListener { button ->
            val input = view.findViewById<TextInputEditText>(R.id.etComment)
            val text = input.text?.toString().orEmpty().trim()
            if (uid.isBlank()) {
                Toast.makeText(requireContext(), getString(R.string.k_com_login), Toast.LENGTH_SHORT).show()
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
                val result = commentRepo.add(requireContext(), uid, publicationId, profile, text)
                if (!isAdded) return@launch
                button.isEnabled = true
                if (result.isSuccess) {
                    input.text?.clear()
                    val ownerUid = currentPublication.ownerUid
                    if (ownerUid.isNotBlank() && ownerUid != uid) {
                        NotificationRepository().push(
                            recipientUid = ownerUid,
                            type = NotificationType.NEW_COMMENT,
                            title = getString(R.string.k_push_comentario),
                            message = getString(R.string.k_push_comentario_fmt, currentPublication.title.take(60)),
                            senderUid = uid,
                            publicationId = publicationId
                        )
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        result.exceptionOrNull()?.message ?: getString(R.string.k_com_no_comentar),
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
            menu.menu.add(0, 1, 0, getString(R.string.k_comun_editar))
            menu.menu.add(0, 2, 0, getString(R.string.k_comun_eliminar))
        } else {
            menu.menu.add(0, 3, 0, getString(R.string.k_comun_denunciar))
        }
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> editarComentario(comment)
                2 -> androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.k_com_eliminar_titulo)
                    .setMessage(R.string.k_com_eliminar_msg)
                    .setPositiveButton(R.string.k_comun_eliminar) { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            val result = commentRepo.delete(requireContext(), uid, comment)
                            if (isAdded && result.isFailure) {
                                Toast.makeText(requireContext(), getString(R.string.k_comun_no_eliminar), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    .setNegativeButton(R.string.k_comun_cancelar, null)
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
            .setTitle(R.string.k_com_editar_titulo)
            .setView(input)
            .setPositiveButton(R.string.k_comun_guardar) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = commentRepo.edit(requireContext(), comment.authorUid, comment.commentId, input.text?.toString().orEmpty())
                    if (isAdded && result.isFailure) {
                        Toast.makeText(
                            requireContext(),
                            result.exceptionOrNull()?.message ?: getString(R.string.k_com_no_editar),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton(R.string.k_comun_cancelar, null)
            .show()
    }

    private fun denunciarComentario(
        comment: com.proyecto.chambaya.data.model.PublicationComment,
        publicationId: String,
        uid: String
    ) {
        if (uid.isBlank()) {
            Toast.makeText(requireContext(), getString(R.string.k_com_denunciar_login), Toast.LENGTH_SHORT).show()
            return
        }
        val reasons = arrayOf(getString(R.string.k_razon_spam), getString(R.string.k_razon_inapropiado), getString(R.string.k_razon_acoso), getString(R.string.k_razon_fraude), getString(R.string.k_razon_otro))
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.k_com_denunciar_titulo)
            .setItems(reasons) { _, index ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = commentRepo.report(publicationId, comment.commentId, uid, reasons[index])
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            if (result.isSuccess) getString(R.string.k_com_denunciar_enviar) else getString(R.string.k_comun_no_enviar),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton(R.string.k_comun_cancelar, null)
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
