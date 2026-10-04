package com.proyecto.chambaya.ui.chat

import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.chatListTime
import com.proyecto.chambaya.data.model.PublicProfile
import com.proyecto.chambaya.data.repository.BlockRepository
import com.proyecto.chambaya.data.repository.ChatRepository
import com.proyecto.chambaya.data.repository.ProfileRepository
import kotlinx.coroutines.launch

class FragmentoMensajes : Fragment() {

    private lateinit var rvChats: RecyclerView
    private lateinit var adapter: AdaptadorConversaciones
    private lateinit var etSearchChats: EditText
    private lateinit var chipAll: TextView
    private lateinit var chipUnread: TextView
    private lateinit var chipFavorites: TextView
    private lateinit var chipAddFilter: FrameLayout
    private lateinit var btnNewChat: FrameLayout
    private lateinit var headerLayout: View

    private var currentFilter = AdaptadorConversaciones.TipoFiltro.TODOS
    private var queryActual = ""
    /** Primera foto del servidor ya integrada: distingue "cargando" de "vacío". */
    private var cargaInicialCompleta = false

    private val chatRepo = ChatRepository()
    private val profileRepo = ProfileRepository()
    private val blockRepo = BlockRepository()
    private val perfilCache = mutableMapOf<String, PublicProfile>()
    private var convListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var emptyView: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragmento_mensajes, container, false)

        initViews(view)
        setupInsets(view)
        setupRecyclerView()
        setupSearch()
        setupFilterChips()
        setupActions()

        return view
    }

    private fun initViews(view: View) {
        rvChats = view.findViewById(R.id.rvChats)
        etSearchChats = view.findViewById(R.id.etSearchChats)
        chipAll = view.findViewById(R.id.chipAll)
        chipUnread = view.findViewById(R.id.chipUnread)
        chipFavorites = view.findViewById(R.id.chipFavorites)
        chipAddFilter = view.findViewById(R.id.chipAddFilter)
        btnNewChat = view.findViewById(R.id.btnNewChat)
        headerLayout = view.findViewById(R.id.headerLayout)
    }

    private fun setupInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            headerLayout.setPadding(
                headerLayout.paddingLeft,
                statusBars.top + dpToPx(8),
                headerLayout.paddingRight,
                headerLayout.paddingBottom
            )
            insets
        }
    }

    private fun setupRecyclerView() {
        rvChats.layoutManager = LinearLayoutManager(requireContext())
        adapter = AdaptadorConversaciones(emptyList()) { chat ->
            abrirDetalleChat(chat)
        }
        rvChats.adapter = adapter
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val chat = adapter.chatAt(position)
                if (chat == null) {
                    adapter.notifyDataSetChanged()
                    return
                }
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                if (uid.isBlank()) {
                    adapter.notifyDataSetChanged()
                    return
                }
                preferenciasChats().edit()
                    .putLong(claveChatOculto(uid, chat.id), chat.lastMessageAtEpochMillis)
                    .apply()
                adapter.quitarConversacion(chat.id)
                if (!adapter.tieneConversaciones()) {
                    mostrarVacio("Sin conversaciones.\nLos chats nacen de tus postulaciones y solicitudes.")
                }
            }
        }).attachToRecyclerView(rvChats)
    }

    private fun abrirDetalleChat(chat: ChatConversacion) {
        val intent = Intent(requireContext(), ActividadChatDetalle::class.java).apply {
            putExtra(ActividadChatDetalle.EXTRA_CONV_ID, chat.id)
            putExtra(ActividadChatDetalle.EXTRA_OTHER_UID, chat.otherUid)
            putExtra(ActividadChatDetalle.EXTRA_NOMBRE, chat.nombre)
            putExtra(ActividadChatDetalle.EXTRA_FOTO, chat.photoUrl)
            putExtra(ActividadChatDetalle.EXTRA_PUB_TITULO, chat.publicationTitle)
        }
        startActivity(intent)
    }

    private fun setupSearch() {
        etSearchChats.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                queryActual = s?.toString() ?: ""
                adapter.filtrarPorTexto(queryActual)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilterChips() {
        // Solo Todos / No leídos tienen sentido con datos reales.
        chipFavorites.visibility = View.GONE
        chipAddFilter.visibility = View.GONE
        chipAll.setOnClickListener {
            seleccionarChip(AdaptadorConversaciones.TipoFiltro.TODOS)
        }

        chipUnread.setOnClickListener {
            seleccionarChip(AdaptadorConversaciones.TipoFiltro.NO_LEIDOS)
        }
    }

    private fun seleccionarChip(tipo: AdaptadorConversaciones.TipoFiltro) {
        currentFilter = tipo

        // Reset visual state of all chips
        val bgInactive = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chat_chip_inactive)
        val bgActive = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chat_chip_active)
        val textActiveColor = ContextCompat.getColor(requireContext(), R.color.chat_chip_active_text)
        val textInactiveColor = ContextCompat.getColor(requireContext(), R.color.chat_chip_inactive_text)

        chipAll.background = if (tipo == AdaptadorConversaciones.TipoFiltro.TODOS) bgActive else bgInactive
        chipAll.setTextColor(if (tipo == AdaptadorConversaciones.TipoFiltro.TODOS) textActiveColor else textInactiveColor)

        chipUnread.background = if (tipo == AdaptadorConversaciones.TipoFiltro.NO_LEIDOS) bgActive else bgInactive
        chipUnread.setTextColor(if (tipo == AdaptadorConversaciones.TipoFiltro.NO_LEIDOS) textActiveColor else textInactiveColor)

        adapter.filtrarPorTipo(tipo)
    }

    // ── FASE 13 · Datos reales en tiempo real ─────────────────────

    override fun onResume() {
        super.onResume()
        try {
            BarraEstadoUtils.aplicarColor(requireActivity(), requireContext().getColor(R.color.white))
        } catch (_: Exception) { }
        attachConversations()
    }

    override fun onPause() {
        convListener?.remove()
        convListener = null
        super.onPause()
    }

    override fun onDestroyView() {
        convListener?.remove()
        convListener = null
        perfilCache.clear()
        emptyView = null
        super.onDestroyView()
    }

    private fun attachConversations() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (uid.isBlank() || convListener != null || !isAdded) return
        convListener = chatRepo.listenMine(
            uid,
            onUpdate = { convs -> integrarConversaciones(uid, convs) },
            onError = { e ->
                // Sin Toast: los errores transitorios al re-enganchar son
                // normales y los datos llegan enseguida. Solo se loguea; si
                // no hay nada cargado se muestra el estado vacío de error.
                android.util.Log.e("FragmentoMensajes", "listenMine falló", e)
                if (!isAdded) return@listenMine
                if (!cargaInicialCompleta && (!::adapter.isInitialized || adapter.itemCount == 0)) {
                    mostrarVacio("No se pudieron cargar los chats.")
                }
            }
        )
    }

    private fun integrarConversaciones(uid: String, convs: List<com.proyecto.chambaya.data.model.Conversation>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val bloques = blockRepo.myBlocks(uid).getOrNull().orEmpty()
            val preferencias = preferenciasChats()
            val items = mutableListOf<ChatConversacion>()
            for (conv in convs) {
                val hiddenAt = preferencias.getLong(claveChatOculto(uid, conv.conversationId), -1L)
                val latestAt = conv.lastMessageAt?.toDate()?.time ?: 0L
                if (hiddenAt >= 0L) {
                    if (latestAt <= hiddenAt) continue
                    preferencias.edit().remove(claveChatOculto(uid, conv.conversationId)).apply()
                }
                val other = conv.otherUid(uid)
                if (other.isBlank() || other in bloques) continue
                var perfil = perfilCache[other]
                if (perfil == null) {
                    perfil = profileRepo.loadPublicProfile(other).getOrNull()
                    if (perfil != null) perfilCache[other] = perfil
                }
                val unread = chatRepo.unreadIn(conv.conversationId, uid)
                items += ChatConversacion(
                    id = conv.conversationId,
                    nombre = perfil?.displayName() ?: "Chat",
                    ultimoMensaje = conv.lastMessage.ifBlank {
                        conv.publicationTitle.takeIf { it.isNotBlank() }?.let { "Chamba: $it" }
                            ?: "Inicia la conversación"
                    },
                    hora = chatListTime(conv.lastMessageAt),
                    noLeidos = unread,
                    photoUrl = perfil?.photoUrl.orEmpty(),
                    otherUid = other,
                    publicationId = conv.publicationId,
                    publicationTitle = conv.publicationTitle,
                    lastMessageAtEpochMillis = latestAt
                )
            }
            if (!isAdded) return@launch
            cargaInicialCompleta = true
            adapter.actualizarYFiltrar(items, currentFilter, queryActual)
            if (items.isEmpty()) mostrarVacio("Sin conversaciones.\nLos chats nacen de tus postulaciones y solicitudes.")
            else ocultarVacio()
        }
    }

    private fun mostrarVacio(texto: String) {
        if (!isAdded) return
        var tv = emptyView
        if (tv == null) {
            tv = TextView(requireContext()).apply {
                gravity = android.view.Gravity.CENTER
                setTextColor(requireContext().getColor(R.color.text_secondary))
                textSize = 14f
                setPadding(48, 48, 48, 48)
            }
            val parent = rvChats.parent as? ViewGroup
            val params = if (parent is androidx.constraintlayout.widget.ConstraintLayout) {
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topToBottom = R.id.filterScrollView
                    bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    verticalBias = 0.3f
                }
            } else {
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            parent?.addView(tv, params)
            emptyView = tv
        }
        tv.text = texto
        tv.visibility = View.VISIBLE
        rvChats.visibility = View.GONE
    }

    private fun ocultarVacio() {
        emptyView?.visibility = View.GONE
        if (::rvChats.isInitialized) rvChats.visibility = View.VISIBLE
    }

    private fun setupActions() {
        btnNewChat.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Los chats se inician desde tus postulaciones o solicitudes.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun preferenciasChats() =
        requireContext().getSharedPreferences("chat_inbox_hidden", Context.MODE_PRIVATE)

    private fun claveChatOculto(uid: String, conversationId: String) = "${uid}_$conversationId"
}
