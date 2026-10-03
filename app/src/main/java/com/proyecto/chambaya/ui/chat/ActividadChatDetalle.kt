package com.proyecto.chambaya.ui.chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.chatBubbleTime
import com.proyecto.chambaya.data.repository.BlockRepository
import com.proyecto.chambaya.data.repository.ChatRepository
import com.proyecto.chambaya.ui.jobs.PublicProfileSheet
import kotlinx.coroutines.launch

/**
 * FASE 13 — Detalle del chat en tiempo real (burbujas y adjuntos ya existían;
 * los mensajes mock se reemplazan por Firestore en vivo).
 */
class ActividadChatDetalle : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessageInput: EditText
    private lateinit var adapter: AdaptadorMensajes
    private var mensajesActuales = mutableListOf<MensajeChat>()

    private val chatRepo = ChatRepository()
    private val blockRepo = BlockRepository()
    private var msgListener: com.google.firebase.firestore.ListenerRegistration? = null

    private var conversationId: String = ""
    private var otherUid: String = ""
    private var myUid: String = ""
    private var otherName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.actividad_chat_detalle)

        BarraEstadoUtils.aplicarColor(this, getColor(R.color.white))

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chatDetailRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomPadding = if (ime.bottom > 0) ime.bottom else systemBars.bottom
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding)
            insets
        }

        conversationId = intent.getStringExtra(EXTRA_CONV_ID).orEmpty()
        otherUid = intent.getStringExtra(EXTRA_OTHER_UID).orEmpty()
        otherName = intent.getStringExtra(EXTRA_NOMBRE) ?: "Chat"
        val photoUrl = intent.getStringExtra(EXTRA_FOTO).orEmpty()
        val pubTitle = intent.getStringExtra(EXTRA_PUB_TITULO).orEmpty()
        myUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        if (conversationId.isBlank() || myUid.isBlank()) {
            Toast.makeText(this, "No se pudo abrir el chat.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupHeader(otherName, photoUrl, pubTitle)
        setupMessages()
        setupInput()
        verificarBloqueo()
    }

    override fun onDestroy() {
        msgListener?.remove()
        super.onDestroy()
    }

    private fun setupHeader(nombre: String, photoUrl: String, pubTitle: String) {
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val ivDetailAvatar = findViewById<ShapeableImageView>(R.id.ivDetailAvatar)
        val viewDetailOnlineDot = findViewById<View>(R.id.viewDetailOnlineDot)
        val tvDetailName = findViewById<TextView>(R.id.tvDetailName)
        val tvDetailStatus = findViewById<TextView>(R.id.tvDetailStatus)
        val btnCall = findViewById<ImageView>(R.id.btnCall)
        val btnMoreOptions = findViewById<ImageView>(R.id.btnMoreOptions)

        tvDetailName.text = nombre
        if (photoUrl.isNotBlank()) {
            ivDetailAvatar.load(photoUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_user_circle)
                error(R.drawable.ic_user_circle)
            }
        } else {
            ivDetailAvatar.setImageResource(R.drawable.ic_user_circle)
        }
        // La lista a veces trae la foto vacía: se reintenta directo del perfil.
        lifecycleScope.launch {
            val fresca = com.proyecto.chambaya.data.repository.ProfileRepository()
                .loadPublicProfile(otherUid).getOrNull()?.photoUrl.orEmpty()
            if (fresca.isNotBlank() && fresca != photoUrl) {
                ivDetailAvatar.load(fresca) {
                    crossfade(true)
                    placeholder(R.drawable.ic_user_circle)
                    error(R.drawable.ic_user_circle)
                }
            }
        }
        // Sin sistema de presencia: el subtítulo muestra el contexto.
        viewDetailOnlineDot.visibility = View.GONE
        tvDetailStatus.text = pubTitle.ifBlank { "Chat de ChambAYA" }

        btnBack.setOnClickListener { finish() }
        btnCall.setOnClickListener {
            Toast.makeText(this, "Las llamadas llegan pronto a ChambAYA.", Toast.LENGTH_SHORT).show()
        }
        btnMoreOptions.setOnClickListener { menuChat(it) }
        ivDetailAvatar.setOnClickListener { verPerfil() }
        tvDetailName.setOnClickListener { verPerfil() }
    }

    private fun verPerfil() {
        if (otherUid.isNotBlank()) {
            PublicProfileSheet.newInstance(otherUid).show(supportFragmentManager, "profile")
        }
    }

    private fun menuChat(anchor: View) {
        val menu = PopupMenu(this, anchor)
        menu.menu.add(0, 1, 0, "Ver perfil")
        menu.menu.add(0, 2, 0, "Bloquear")
        menu.menu.add(0, 3, 0, "Denunciar")
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> verPerfil()
                2 -> confirmarBloqueo()
                3 -> denunciar()
            }
            true
        }
        menu.show()
    }

    private fun confirmarBloqueo() {
        AlertDialog.Builder(this)
            .setTitle("Bloquear a $otherName")
            .setMessage("No verás sus chambas ni podrán escribirse. Podrás desbloquearlo desde su perfil.")
            .setPositiveButton("Bloquear") { _, _ ->
                lifecycleScope.launch {
                    val r = blockRepo.block(myUid, otherUid)
                    Toast.makeText(
                        this@ActividadChatDetalle,
                        if (r.isSuccess) "Usuario bloqueado." else "No se pudo bloquear.",
                        Toast.LENGTH_SHORT
                    ).show()
                    if (r.isSuccess) finish()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun denunciar() {
        val motivos = arrayOf("Spam", "Acoso", "Fraude o estafa", "Contenido inapropiado", "Otro")
        AlertDialog.Builder(this)
            .setTitle("Denunciar a $otherName")
            .setItems(motivos) { _, cual ->
                lifecycleScope.launch {
                    val r = blockRepo.reportUser(myUid, otherUid, motivos[cual])
                    Toast.makeText(
                        this@ActividadChatDetalle,
                        if (r.isSuccess) "Denuncia enviada. La revisaremos." else "No se pudo enviar.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun verificarBloqueo() {
        lifecycleScope.launch {
            val bloqueado = blockRepo.isBlocked(myUid, otherUid) ||
                blockRepo.isBlocked(otherUid, myUid)
            if (bloqueado) {
                etMessageInput.isEnabled = false
                etMessageInput.hint = "Chat no disponible"
                findViewById<View>(R.id.btnSend).visibility = View.GONE
                Toast.makeText(this@ActividadChatDetalle, "Este chat está bloqueado.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupMessages() {
        rvMessages = findViewById(R.id.rvMessages)
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        adapter = AdaptadorMensajes(mensajesActuales)
        rvMessages.adapter = adapter

        msgListener?.remove()
        msgListener = chatRepo.listenMessages(
            conversationId,
            onUpdate = { lista ->
                val lm = this@ActividadChatDetalle.rvMessages.layoutManager as? LinearLayoutManager
                val estabaAbajo = lm == null ||
                    lm.findLastCompletelyVisibleItemPosition() >= adapter.itemCount - 2
                val nuevos = lista.map {
                    MensajeChat(
                        id = it.messageId,
                        texto = it.text,
                        hora = chatBubbleTime(it.createdAt),
                        esMio = it.senderUid == myUid,
                        estaLeido = it.read || it.senderUid == myUid
                    )
                }
                mensajesActuales = nuevos.toMutableList()
                adapter.actualizarMensajes(mensajesActuales)
                if (estabaAbajo && nuevos.isNotEmpty()) {
                    rvMessages.scrollToPosition(nuevos.size - 1)
                }
                // Marca leídos los del otro en segundo plano.
                lifecycleScope.launch { chatRepo.markRead(myUid, conversationId) }
            },
            onError = {
                Toast.makeText(this, "Se cortó la conexión del chat.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupInput() {
        etMessageInput = findViewById(R.id.etMessageInput)
        val btnAttachInline = findViewById<ImageView>(R.id.btnAttachInline)
        val btnSend = findViewById<FrameLayout>(R.id.btnSend)

        btnAttachInline.setOnClickListener { abrirMenuAdjuntos() }
        btnSend.setOnClickListener { enviarMensajeActual() }

        etMessageInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnSend.visibility = if (s?.trim()?.isNotEmpty() == true) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        etMessageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            ) {
                enviarMensajeActual()
                true
            } else {
                false
            }
        }
    }

    private fun abrirMenuAdjuntos() {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_adjuntos, null)
        dialog.setContentView(sheetView)
        val pronto = {
            Toast.makeText(this, "Fotos y archivos llegan pronto.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        sheetView.findViewById<LinearLayout>(R.id.btnMenuGaleria)?.setOnClickListener { pronto() }
        sheetView.findViewById<LinearLayout>(R.id.btnMenuCamara)?.setOnClickListener { pronto() }
        sheetView.findViewById<LinearLayout>(R.id.btnMenuUbicacion)?.setOnClickListener { pronto() }
        sheetView.findViewById<LinearLayout>(R.id.btnMenuDocumento)?.setOnClickListener { pronto() }
        dialog.show()
    }

    private fun enviarMensajeActual() {
        val texto = etMessageInput.text?.toString()?.trim().orEmpty()
        if (texto.isEmpty()) return
        etMessageInput.text?.clear()
        findViewById<View>(R.id.btnSend).visibility = View.GONE
        lifecycleScope.launch {
            val r = chatRepo.sendMessage(myUid, conversationId, texto)
            if (r.isFailure) {
                Toast.makeText(
                    this@ActividadChatDetalle,
                    r.exceptionOrNull()?.message ?: "No se pudo enviar.",
                    Toast.LENGTH_SHORT
                ).show()
                etMessageInput.setText(texto)
            }
        }
    }

    companion object {
        const val EXTRA_CONV_ID = "extra_conv_id"
        const val EXTRA_OTHER_UID = "extra_other_uid"
        const val EXTRA_NOMBRE = "extra_nombre"
        const val EXTRA_FOTO = "extra_foto"
        const val EXTRA_PUB_TITULO = "extra_pub_titulo"
        // Compatibilidad con llamadas antiguas:
        const val EXTRA_AVATAR = "extra_avatar"
        const val EXTRA_ONLINE = "extra_online"
    }
}
