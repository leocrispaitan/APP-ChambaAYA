package com.proyecto.chambaya.ui.jobs

import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.AppNotification
import com.proyecto.chambaya.data.model.NotificationType
import com.proyecto.chambaya.data.model.publicationTimeAgo
import com.proyecto.chambaya.data.repository.NotificationRepository
import kotlinx.coroutines.launch

/**
 * Bandeja de notificaciones: abrir con tap, deslizar a la izquierda para
 * eliminar una (con Deshacer) y papelera para borrar todas (con confirmación).
 */
class NotificationsSheet : BottomSheetDialogFragment() {

    private val repo = NotificationRepository()
    private var adapter: Adapter? = null
    private var registration: com.google.firebase.firestore.ListenerRegistration? = null
    private var backCallback: androidx.activity.OnBackPressedCallback? = null

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
                if (adapter?.modoSeleccion == true) {
                    alternarSeleccion(n)
                } else if (n.publicationId.isNotBlank()) {
                    parentFragmentManager.setFragmentResult(
                        REQUEST_OPEN_PUB, bundleOf(EXTRA_PUB to n.publicationId)
                    )
                    dismiss()
                }
            },
            onLongPress = { n, anchor -> mostrarMenuPulsacion(uid, n, anchor) }
        )
        rv.adapter = adapter
        setupSwipeToDelete(rv, uid)
        setupBarraSeleccion(uid)
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
                val result = repo.markAllRead(uid)
                if (!isAdded) return@launch
                v.isEnabled = true
                if (result.isFailure) {
                    Toast.makeText(requireContext(), "No se pudieron marcar como leídas.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        view.findViewById<View>(R.id.btnNotifDeleteAll).setOnClickListener {
            confirmarBorrarTodas(uid)
        }
    }

    /** Menú de pulsación larga: leído/no leído, eliminar y seleccionar. */
    private fun mostrarMenuPulsacion(uid: String, n: AppNotification, anchor: View) {
        val menu = androidx.appcompat.widget.PopupMenu(requireContext(), anchor)
        menu.menu.add(0, 1, 0, if (n.read) "Marcar como no leído" else "Marcar como leído")
        menu.menu.add(0, 2, 0, "Eliminar")
        menu.menu.add(0, 3, 0, "Seleccionar")
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val result = repo.setRead(uid, listOf(n.notificationId), !n.read)
                        if (!isAdded) return@launch
                        Toast.makeText(
                            requireContext(),
                            when {
                                result.isFailure -> "No se pudo actualizar la notificación."
                                n.read -> "Notificación marcada como no leída."
                                else -> "Notificación marcada como leída."
                            },
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    true
                }
                2 -> {
                    eliminarUno(uid, n)
                    true
                }
                3 -> {
                    entrarSeleccion(n)
                    true
                }
                else -> false
            }
        }
        menu.show()
    }

    /** Borra un aviso con Deshacer (la lista en vivo se repinta sola). */
    private fun eliminarUno(uid: String, n: AppNotification, pos: Int? = null) {
        viewLifecycleOwner.lifecycleScope.launch {
            val r = repo.deleteOne(uid, n.notificationId)
            if (!isAdded) return@launch
            if (r.isSuccess) {
                Snackbar.make(requireView(), "Notificación eliminada", Snackbar.LENGTH_LONG)
                    .setAction("Deshacer") {
                        viewLifecycleOwner.lifecycleScope.launch {
                            repo.restore(uid, n)
                        }
                    }
                    .show()
            } else {
                if (pos != null) adapter?.notifyItemChanged(pos)
                Toast.makeText(requireContext(), "No se pudo eliminar.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Selección múltiple ───────────────────────────────────

    private fun entrarSeleccion(n: AppNotification) {
        adapter?.entrarSeleccion(n)
        mostrarBarraSeleccion()
    }

    private fun alternarSeleccion(n: AppNotification) {
        val sigue = adapter?.alternar(n) == true
        if (sigue) actualizarBarraSeleccion() else ocultarBarraSeleccion()
    }

    private fun mostrarBarraSeleccion() {
        view?.findViewById<View>(R.id.layoutNotifHeader)?.visibility = View.GONE
        view?.findViewById<View>(R.id.layoutNotifSelection)?.visibility = View.VISIBLE
        actualizarBarraSeleccion()
        backCallback?.isEnabled = true
    }

    private fun actualizarBarraSeleccion() {
        val n = adapter?.seleccionados?.size ?: 0
        view?.findViewById<TextView>(R.id.tvSelCount)?.text =
            if (n == 1) "1 seleccionada" else "$n seleccionadas"
        view?.findViewById<TextView>(R.id.btnSelAll)?.text =
            if (n > 0 && n == adapter?.itemCount) "Ninguna" else "Todo"
    }

    private fun salirSeleccion() {
        adapter?.limpiarSeleccion()
        ocultarBarraSeleccion()
    }

    private fun ocultarBarraSeleccion() {
        view?.findViewById<View>(R.id.layoutNotifSelection)?.visibility = View.GONE
        view?.findViewById<View>(R.id.layoutNotifHeader)?.visibility = View.VISIBLE
        backCallback?.isEnabled = false
    }

    private fun setupBarraSeleccion(uid: String) {
        backCallback = object : androidx.activity.OnBackPressedCallback(false) {
            override fun handleOnBackPressed() = salirSeleccion()
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback!!)
        view?.findViewById<View>(R.id.btnSelBack)?.setOnClickListener { salirSeleccion() }
        view?.findViewById<View>(R.id.btnSelAll)?.setOnClickListener {
            val a = adapter ?: return@setOnClickListener
            if (a.seleccionados.size == a.itemCount && a.itemCount > 0) salirSeleccion()
            else {
                a.seleccionarTodo()
                actualizarBarraSeleccion()
            }
        }
        view?.findViewById<View>(R.id.btnSelRead)?.setOnClickListener {
            val ids = adapter?.seleccionados?.toList().orEmpty()
            if (ids.isEmpty()) return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                val r = repo.setRead(uid, ids, true)
                if (!isAdded) return@launch
                Toast.makeText(
                    requireContext(),
                    if (r.isSuccess) "Marcadas como leídas." else "No se pudo actualizar.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        view?.findViewById<View>(R.id.btnSelDelete)?.setOnClickListener {
            val lista = adapter?.avisosSeleccionados().orEmpty()
            if (lista.isEmpty()) return@setOnClickListener
            salirSeleccion()
            viewLifecycleOwner.lifecycleScope.launch {
                val resultados = lista.map { it to repo.deleteOne(uid, it.notificationId) }
                if (!isAdded) return@launch
                val eliminadas = resultados.filter { it.second.isSuccess }.map { it.first }
                val fallidas = resultados.size - eliminadas.size
                if (fallidas > 0) {
                    Toast.makeText(
                        requireContext(),
                        "$fallidas notificación(es) no se pudieron eliminar.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                if (eliminadas.isEmpty()) return@launch
                Snackbar.make(
                    requireView(),
                    if (lista.size == 1) "1 notificación eliminada"
                    else "${lista.size} notificaciones eliminadas",
                    Snackbar.LENGTH_LONG
                )
                    .setAction("Deshacer") {
                        viewLifecycleOwner.lifecycleScope.launch {
                            eliminadas.forEach { repo.restore(uid, it) }
                        }
                    }
                    .show()
            }
        }
    }

    /** Papelera superior: borra todo con confirmación previa. */
    private fun confirmarBorrarTodas(uid: String) {
        if (adapter?.itemCount == 0) {
            Toast.makeText(requireContext(), "No hay notificaciones.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar notificaciones")
            .setMessage("Se borrarán todas tus notificaciones. Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar todo") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val r = repo.deleteAll(uid)
                    if (!isAdded) return@launch
                    Toast.makeText(
                        requireContext(),
                        if (r.isSuccess) "Notificaciones eliminadas." else "No se pudo eliminar.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Deslizar a la izquierda = eliminar con fondo rojo + Deshacer. */
    private fun setupSwipeToDelete(rv: RecyclerView, uid: String) {
        val fondo = ColorDrawable(0)
        val icono = ContextCompat.getDrawable(requireContext(), R.drawable.ic_notif_delete)
        val rojo = requireContext().getColor(R.color.notif_swipe_delete_bg)
        val blanco = requireContext().getColor(R.color.white)
        val density = resources.displayMetrics.density

        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val pos = vh.bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return
                val aviso = adapter?.currentList?.getOrNull(pos) ?: return
                if (adapter?.modoSeleccion == true) {
                    // En selección no se elimina por swipe: se restaura la fila.
                    adapter?.notifyItemChanged(pos)
                    return
                }
                eliminarUno(uid, aviso, pos)
            }

            override fun onChildDraw(
                c: Canvas,
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    val item = vh.itemView
                    fondo.color = rojo
                    fondo.setBounds(
                        (item.right + dX).toInt(),
                        (item.top + 8 * density).toInt(),
                        item.right,
                        (item.bottom - 8 * density).toInt()
                    )
                    fondo.draw(c)
                    icono?.let {
                        val lado = (24 * density).toInt()
                        val margen = (20 * density).toInt()
                        val arriba = item.top + (item.height - lado) / 2
                        it.setBounds(
                            item.right - margen - lado,
                            arriba,
                            item.right - margen,
                            arriba + lado
                        )
                        it.setTint(blanco)
                        it.draw(c)
                    }
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(rv)
    }

    override fun onDestroyView() {
        registration?.remove()
        registration = null
        adapter = null
        backCallback = null
        super.onDestroyView()
    }

    class Adapter(
        private val onOpen: (AppNotification) -> Unit,
        private val onLongPress: (AppNotification, View) -> Unit
    ) : ListAdapter<AppNotification, Adapter.VH>(Diff()) {
        /** Ids en selección múltiple (pulsación larga). */
        val seleccionados = mutableSetOf<String>()
        var modoSeleccion = false
            private set

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
            return VH(v, onOpen, onLongPress, ::estaSeleccionado)
        }
        override fun onBindViewHolder(h: VH, p: Int) = h.bind(getItem(p))

        fun estaSeleccionado(id: String): Boolean = id in seleccionados

        /** Entra en modo selección con este aviso marcado. */
        fun entrarSeleccion(n: AppNotification) {
            modoSeleccion = true
            seleccionados.add(n.notificationId)
            notifyDataSetChanged()
        }

        /** Marca/desmarca en modo selección. Devuelve true si sigue en modo. */
        fun alternar(n: AppNotification): Boolean {
            val id = n.notificationId
            if (id in seleccionados) seleccionados.remove(id) else seleccionados.add(id)
            if (seleccionados.isEmpty()) {
                modoSeleccion = false
            }
            notifyDataSetChanged()
            return modoSeleccion
        }

        fun seleccionarTodo() {
            seleccionados.clear()
            currentList.forEach { seleccionados.add(it.notificationId) }
            notifyDataSetChanged()
        }

        fun limpiarSeleccion() {
            seleccionados.clear()
            modoSeleccion = false
            notifyDataSetChanged()
        }

        fun avisosSeleccionados(): List<AppNotification> {
            val ids = seleccionados.toSet()
            return currentList.filter { it.notificationId in ids }
        }

        class VH(
            view: View,
            private val onOpen: (AppNotification) -> Unit,
            private val onLongPress: (AppNotification, View) -> Unit,
            private val esSeleccionado: (String) -> Boolean
        ) : RecyclerView.ViewHolder(view) {
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
                itemView.setBackgroundColor(
                    if (esSeleccionado(n.notificationId)) itemView.context.getColor(R.color.notif_selected_bg)
                    else android.graphics.Color.TRANSPARENT
                )
                itemView.setOnClickListener { onOpen(n) }
                itemView.setOnLongClickListener {
                    onLongPress(n, it)
                    true
                }
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
