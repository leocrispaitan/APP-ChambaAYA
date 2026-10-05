package com.proyecto.chambaya.ui.publish

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import com.proyecto.chambaya.R
import java.util.Locale

/** Vista de demostración: no procesa ni almacena información de pago. */
class ConfirmarPagoDemoSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_demo_checkout, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        val title = args.getString(ARG_TITLE).orEmpty().ifBlank { getString(R.string.pub_titulo_pantalla) }
        val district = args.getString(ARG_DISTRICT).orEmpty()
        val featured = args.getBoolean(ARG_FEATURED)
        val amount = args.getDouble(ARG_AMOUNT)

        view.findViewById<TextView>(R.id.tvDemoPublicationTitle).text = title
        view.findViewById<TextView>(R.id.tvDemoPublicationType).text = getString(
            if (featured) R.string.pub_tipo_destacada else R.string.pub_tipo_estandar
        )
        view.findViewById<TextView>(R.id.tvDemoPublicationLocation).text =
            district.ifBlank { getString(R.string.demo_pago_sin_distrito) }
        view.findViewById<TextView>(R.id.tvDemoPaymentTotal).text =
            String.format(Locale("es", "PE"), "S/ %.2f", amount)

        val creditCard = view.findViewById<MaterialCardView>(R.id.cardDemoCredit)
        val debitCard = view.findViewById<MaterialCardView>(R.id.cardDemoDebit)
        val creditRadio = view.findViewById<RadioButton>(R.id.radioDemoCredit)
        val debitRadio = view.findViewById<RadioButton>(R.id.radioDemoDebit)
        fun selectCredit(credit: Boolean) {
            creditRadio.isChecked = credit
            debitRadio.isChecked = !credit
            creditCard.setCardBackgroundColor(requireContext().getColor(if (credit) R.color.chat_title else R.color.background_light))
            creditCard.strokeColor = requireContext().getColor(if (credit) R.color.brand_color else R.color.divider)
            debitCard.setCardBackgroundColor(requireContext().getColor(if (credit) R.color.background_light else R.color.brand_container))
            debitCard.strokeColor = requireContext().getColor(if (credit) R.color.divider else R.color.brand_color)
        }
        creditCard.setOnClickListener { selectCredit(true) }
        creditRadio.setOnClickListener { selectCredit(true) }
        debitCard.setOnClickListener { selectCredit(false) }
        debitRadio.setOnClickListener { selectCredit(false) }
        selectCredit(true)

        view.findViewById<View>(R.id.btnConfirmDemoPayment).setOnClickListener {
            parentFragmentManager.setFragmentResult(REQUEST, bundleOf(EXTRA_CONFIRMED to true))
            dismiss()
        }
    }

    override fun onCancel(dialog: android.content.DialogInterface) {
        parentFragmentManager.setFragmentResult(REQUEST, bundleOf(EXTRA_CONFIRMED to false))
        super.onCancel(dialog)
    }

    companion object {
        const val REQUEST = "demo_checkout_confirmation"
        const val EXTRA_CONFIRMED = "confirmed"
        private const val ARG_TITLE = "title"
        private const val ARG_DISTRICT = "district"
        private const val ARG_FEATURED = "featured"
        private const val ARG_AMOUNT = "amount"

        fun newInstance(title: String, district: String, featured: Boolean, amount: Double) =
            ConfirmarPagoDemoSheet().apply {
                arguments = bundleOf(
                    ARG_TITLE to title,
                    ARG_DISTRICT to district,
                    ARG_FEATURED to featured,
                    ARG_AMOUNT to amount
                )
            }
    }
}
