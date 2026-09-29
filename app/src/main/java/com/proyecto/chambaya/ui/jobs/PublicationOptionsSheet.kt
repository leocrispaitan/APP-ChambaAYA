package com.proyecto.chambaya.ui.jobs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.proyecto.chambaya.R

/**
 * FASE 6 — Menú ⋮ de la publicación (reutiliza dialog_job_options.xml).
 *
 * Comunica la acción con Fragment Result para que el Fragment dueño ejecute
 * la lógica real (guardar, compartir, ocultar, denunciar).
 */
class PublicationOptionsSheet : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.dialog_job_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val publicationId = requireArguments().getString(ARG_ID).orEmpty()
        val saved = requireArguments().getBoolean(ARG_SAVED)

        // Etiqueta dinámica Guardar / Guardado.
        val rowSave = view.findViewById<View>(R.id.optionSave) as? ViewGroup
        rowSave?.let { row ->
            // El TextView es el segundo hijo del LinearLayout.
            (0 until row.childCount)
                .map { row.getChildAt(it) }
                .filterIsInstance<TextView>()
                .firstOrNull()
                ?.text = if (saved) "Quitar de guardados" else "Guardar publicación"
        }

        view.findViewById<View>(R.id.optionSave)?.setOnClickListener { emitir(ACTION_SAVE, publicationId); dismiss() }
        view.findViewById<View>(R.id.optionShare)?.setOnClickListener { emitir(ACTION_SHARE, publicationId); dismiss() }
        view.findViewById<View>(R.id.optionWhy)?.setOnClickListener { emitir(ACTION_WHY, publicationId); dismiss() }
        view.findViewById<View>(R.id.optionRate)?.setOnClickListener { emitir(ACTION_RATE, publicationId); dismiss() }
        view.findViewById<View>(R.id.optionNotInterested)?.setOnClickListener { emitir(ACTION_HIDE, publicationId); dismiss() }
        view.findViewById<View>(R.id.optionReport)?.setOnClickListener { emitir(ACTION_REPORT, publicationId); dismiss() }
    }

    private fun emitir(action: String, publicationId: String) {
        parentFragmentManager.setFragmentResult(
            REQUEST,
            bundleOf(EXTRA_ACTION to action, EXTRA_ID to publicationId)
        )
    }

    companion object {
        const val REQUEST = "publication_options"
        const val EXTRA_ACTION = "action"
        const val EXTRA_ID = "publicationId"
        const val ACTION_SAVE = "save"
        const val ACTION_SHARE = "share"
        const val ACTION_WHY = "why"
        const val ACTION_RATE = "rate"
        const val ACTION_HIDE = "hide"
        const val ACTION_REPORT = "report"
        private const val ARG_ID = "id"
        private const val ARG_SAVED = "saved"

        fun newInstance(publicationId: String, title: String, saved: Boolean): PublicationOptionsSheet {
            return PublicationOptionsSheet().apply {
                arguments = bundleOf(ARG_ID to publicationId, ARG_SAVED to saved)
            }
        }
    }
}
