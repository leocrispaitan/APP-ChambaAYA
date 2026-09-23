package com.proyecto.chambaya.ui.chat

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActividadChatDetalle : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessageInput: EditText
    private lateinit var adapter: AdaptadorMensajes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.actividad_chat_detalle)

        BarraEstadoUtils.aplicarColor(this, getColor(R.color.white))

        // Insets handling
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chatDetailRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomPadding = if (ime.bottom > 0) ime.bottom else systemBars.bottom
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding)
            insets
        }

        val nombre = intent.getStringExtra(EXTRA_NOMBRE) ?: "Alex Thompson"
        val avatarResId = intent.getIntExtra(EXTRA_AVATAR, R.drawable.avatar_alex)
        val estaEnLinea = intent.getBooleanExtra(EXTRA_ONLINE, true)

        setupHeader(nombre, avatarResId, estaEnLinea)
        setupMessages()
        setupInput()
    }

    private fun setupHeader(nombre: String, avatarRes: Int, estaEnLinea: Boolean) {
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val ivDetailAvatar = findViewById<ShapeableImageView>(R.id.ivDetailAvatar)
        val viewDetailOnlineDot = findViewById<android.view.View>(R.id.viewDetailOnlineDot)
        val tvDetailName = findViewById<TextView>(R.id.tvDetailName)
        val tvDetailStatus = findViewById<TextView>(R.id.tvDetailStatus)
        val btnCall = findViewById<ImageView>(R.id.btnCall)
        val btnMoreOptions = findViewById<ImageView>(R.id.btnMoreOptions)

        tvDetailName.text = nombre
        ivDetailAvatar.setImageResource(avatarRes)
        viewDetailOnlineDot.visibility = if (estaEnLinea) android.view.View.VISIBLE else android.view.View.GONE
        tvDetailStatus.text = if (estaEnLinea) getString(R.string.chat_status_online) else "Offline"

        btnBack.setOnClickListener {
            finish()
        }

        btnCall.setOnClickListener {
            Toast.makeText(this, "Llamando a $nombre...", Toast.LENGTH_SHORT).show()
        }

        btnMoreOptions.setOnClickListener {
            Toast.makeText(this, "Opciones de chat", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMessages() {
        rvMessages = findViewById(R.id.rvMessages)
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }

        // Exact mock messages from the design image
        val mensajesMock = mutableListOf(
            MensajeChat(
                id = "1",
                texto = "Hey! How are you?",
                hora = "2:15 PM",
                esMio = false
            ),
            MensajeChat(
                id = "2",
                texto = "Hi Alex! I'm good, thanks!\nHow about you?",
                hora = "2:16 PM",
                esMio = true,
                estaLeido = true
            ),
            MensajeChat(
                id = "3",
                texto = "Great! Just finished work\n🎉",
                hora = "2:16 PM",
                esMio = false
            ),
            MensajeChat(
                id = "4",
                texto = "Nice! Wanna grab coffee\nlater?",
                hora = "2:17 PM",
                esMio = true,
                estaLeido = true
            ),
            MensajeChat(
                id = "5",
                texto = "Sounds perfect! Where?",
                hora = "2:18 PM",
                esMio = false
            )
        )

        adapter = AdaptadorMensajes(mensajesMock)
        rvMessages.adapter = adapter
    }

    private fun setupInput() {
        etMessageInput = findViewById(R.id.etMessageInput)
        val btnAttachment = findViewById<FrameLayout>(R.id.btnAttachment)
        val btnEmoji = findViewById<ImageView>(R.id.btnEmoji)
        val btnCamera = findViewById<ImageView>(R.id.btnCamera)

        btnAttachment.setOnClickListener {
            Toast.makeText(this, "Adjuntar archivo", Toast.LENGTH_SHORT).show()
        }

        btnEmoji.setOnClickListener {
            Toast.makeText(this, "Emojis", Toast.LENGTH_SHORT).show()
        }

        btnCamera.setOnClickListener {
            Toast.makeText(this, "Cámara", Toast.LENGTH_SHORT).show()
        }

        etMessageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                enviarMensajeActual()
                true
            } else {
                false
            }
        }
    }

    private fun enviarMensajeActual() {
        val texto = etMessageInput.text?.toString()?.trim() ?: ""
        if (texto.isNotEmpty()) {
            val horaActual = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            val nuevoMensaje = MensajeChat(
                id = System.currentTimeMillis().toString(),
                texto = texto,
                hora = horaActual,
                esMio = true,
                estaLeido = true
            )
            adapter.agregarMensaje(nuevoMensaje)
            rvMessages.scrollToPosition(adapter.itemCount - 1)
            etMessageInput.text?.clear()
        }
    }

    companion object {
        const val EXTRA_NOMBRE = "extra_nombre"
        const val EXTRA_AVATAR = "extra_avatar"
        const val EXTRA_ONLINE = "extra_online"
    }
}
