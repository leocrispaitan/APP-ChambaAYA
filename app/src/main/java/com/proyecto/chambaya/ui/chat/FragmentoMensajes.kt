package com.proyecto.chambaya.ui.chat

import android.content.Intent
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.R

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

        // Mock data identical to the left screen of the image
        val conversaciones = listOf(
            ChatConversacion(
                id = "1",
                nombre = "Alex Thompson",
                ultimoMensaje = "How's yesterday meet-up?",
                hora = "12:27 PM",
                noLeidos = 1,
                avatarResId = R.drawable.avatar_alex,
                estaEnLinea = true,
                esFavorito = true
            ),
            ChatConversacion(
                id = "2",
                nombre = "Jordan Lee",
                ultimoMensaje = "Looking forward to our projec...",
                hora = "1:15 PM",
                noLeidos = 2,
                avatarResId = R.drawable.avatar_jordan,
                estaEnLinea = false,
                esFavorito = true
            ),
            ChatConversacion(
                id = "3",
                nombre = "Samantha Green",
                ultimoMensaje = "Can we discuss the design fe...",
                hora = "2:42 PM",
                noLeidos = 3,
                avatarResId = R.drawable.avatar_samantha,
                estaEnLinea = false,
                esFavorito = true
            ),
            ChatConversacion(
                id = "4",
                nombre = "Michael Brown",
                ultimoMensaje = "Will you be attending the wor...",
                hora = "3:05 PM",
                noLeidos = 4,
                avatarResId = R.drawable.avatar_michael,
                estaEnLinea = false,
                esFavorito = true
            )
        )

        adapter = AdaptadorConversaciones(conversaciones) { chat ->
            abrirDetalleChat(chat)
        }
        rvChats.adapter = adapter
    }

    private fun abrirDetalleChat(chat: ChatConversacion) {
        val intent = Intent(requireContext(), ActividadChatDetalle::class.java).apply {
            putExtra(ActividadChatDetalle.EXTRA_NOMBRE, chat.nombre)
            putExtra(ActividadChatDetalle.EXTRA_AVATAR, chat.avatarResId)
            putExtra(ActividadChatDetalle.EXTRA_ONLINE, chat.estaEnLinea)
        }
        startActivity(intent)
    }

    private fun setupSearch() {
        etSearchChats.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filtrarPorTexto(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilterChips() {
        chipAll.setOnClickListener {
            seleccionarChip(AdaptadorConversaciones.TipoFiltro.TODOS)
        }

        chipUnread.setOnClickListener {
            seleccionarChip(AdaptadorConversaciones.TipoFiltro.NO_LEIDOS)
        }

        chipFavorites.setOnClickListener {
            seleccionarChip(AdaptadorConversaciones.TipoFiltro.FAVORITOS)
        }

        chipAddFilter.setOnClickListener {
            Toast.makeText(requireContext(), "Añadir filtro personalizado", Toast.LENGTH_SHORT).show()
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

        chipFavorites.background = if (tipo == AdaptadorConversaciones.TipoFiltro.FAVORITOS) bgActive else bgInactive
        chipFavorites.setTextColor(if (tipo == AdaptadorConversaciones.TipoFiltro.FAVORITOS) textActiveColor else textInactiveColor)

        adapter.filtrarPorTipo(tipo)
    }

    private fun setupActions() {
        btnNewChat.setOnClickListener {
            Toast.makeText(requireContext(), "Iniciar nueva conversación", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onResume() {
        super.onResume()
        BarraEstadoUtils.aplicarColor(requireActivity(), requireContext().getColor(R.color.white))
    }
}