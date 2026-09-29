package com.proyecto.chambaya.ui.jobs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.AppNotification
import com.proyecto.chambaya.data.model.NotificationType
import com.proyecto.chambaya.data.model.publicationTimeAgo
import com.proyecto.chambaya.data.repository.NotificationRepository
import kotlinx.coroutines.launch

/**
 * Bandeja básica de notificaciones (FASE 7 crea los avisos; push y centro
 * completo de notificaciones es FASE 14).
 */
class NotificationsSheet : BottomSheetDialogFragment() {

    private val repo = NotificationRepository()
    private var adapter: Adapter? = null
    private var registration: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottom_sheet_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (uid.isBlank()) { dismiss(); return }

        val rv = view.findViewById<RecyclerView>(R.id.rvNotifications)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = Adapter(
            onOpen = { n ->
                if (n.publicationId.isNotBlank()) {
                    parentFragmentManager.setFragmentResult(
                        REQUEST_OPEN_PUB, bundleOf(EXTRA_PUB to n.publicationId)
                    )
                    dismiss()
                }
            }
        )
        rv.adapter = adapter
        val progress = view.findViewById<ProgressBar>(R.id.progressNotif)
        val empty = view.findViewById<TextView>(R.id.tvNotifEmpty)
        progress.visibility = View.VISIBLE

        registration = repo.listenMine(
            uid, 30,
            onUpdate = { list ->
                if (!isAdded) return@listenMine
                progress.visibility = View.GONE
                empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                adapter?.submitList(list)
            },
            onError = {
                if (!isAdded) return@listenMine
                progress.visibility = View.GONE
                empty.visibility = View.VISIBLE
            }
        )
        view.findViewById<View>(R.id.btnNotifReadAll).setOnClickListener { v ->
            v.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                repo.markAllRead(uid)
                if (isAdded) v.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        registration?.remove()
        registration = null
        adapter = null
        super.onDestroyView()
    }

    class Adapter(
        private val onOpen: (AppNotification) -> Unit
    ) : ListAdapter<AppNotification, Adapter.VH>(Diff()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
            return VH(v, onOpen)
        }
        override fun onBindViewHolder(h: VH, p: Int) = h.bind(getItem(p))

        class VH(view: View, private val onOpen: (AppNotification) -> Unit) :
            RecyclerView.ViewHolder(view) {
            private val dot: View = view.findViewById(R.id.dotUnread)
            private val tvTitle: TextView = view.findViewById(R.id.tvNotifTitle)
            private val tvMsg: TextView = view.findViewById(R.id.tvNotifMessage)
            private val tvTime: TextView = view.findViewById(R.id.tvNotifTime)
            fun bind(n: AppNotification) {
                tvTitle.text = n.title.ifBlank {
                    when (n.type) {
                        NotificationType.NEW_APPLICATION -> "Nueva postulación"
                        NotificationType.APPLICATION_ACCEPTED -> "¡Fuiste seleccionado!"
                        NotificationType.APPLICATION_REJECTED -> "Postulación decidida"
                        else -> "Aviso"
                    }
                }
                tvMsg.text = n.message
                tvTime.text = publicationTimeAgo(n.createdAt)
                dot.visibility = if (n.read) View.INVISIBLE else View.VISIBLE
                itemView.setOnClickListener { onOpen(n) }
            }
        }
        class Diff : DiffUtil.ItemCallback<AppNotification>() {
            override fun areItemsTheSame(a: AppNotification, b: AppNotification): Boolean =
                a.notificationId == b.notificationId
            override fun areContentsTheSame(a: AppNotification, b: AppNotification): Boolean = a == b
        }
    }

    companion object {
        const val REQUEST_OPEN_PUB = "notif_open_pub"
        const val EXTRA_PUB = "publicationId"
    }
}
