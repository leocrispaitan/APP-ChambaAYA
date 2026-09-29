package com.proyecto.chambaya.ui.jobs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import com.proyecto.chambaya.R

/**
 * FASE 6 — Filtros del feed (categoría, distrito, pago mínimo, orden).
 * Devuelve el resultado con Fragment Result.
 */
class FilterBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottom_sheet_filters, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val categories = requireArguments().getStringArrayList(ARG_CATS).orEmpty()
        val districts = requireArguments().getStringArrayList(ARG_DIST).orEmpty()
        val current = PublicationFilters(
            category = requireArguments().getString(ARG_CAT).orEmpty(),
            district = requireArguments().getString(ARG_DIS).orEmpty(),
            minAmount = requireArguments().getDouble(ARG_MIN),
            sortNewestFirst = requireArguments().getBoolean(ARG_SORT, true)
        )

        val groupCat = view.findViewById<ChipGroup>(R.id.chipGroupCategory)
        categories.take(12).forEach { cat ->
            val chip = Chip(requireContext()).apply {
                text = cat; isCheckable = true
                isChecked = cat.equals(current.category, ignoreCase = true)
                tag = cat
            }
            groupCat.addView(chip)
        }
        val groupDis = view.findViewById<ChipGroup>(R.id.chipGroupDistrict)
        districts.take(12).forEach { dis ->
            val chip = Chip(requireContext()).apply {
                text = dis; isCheckable = true
                isChecked = dis.equals(current.district, ignoreCase = true)
                tag = dis
            }
            groupDis.addView(chip)
        }

        val slider = view.findViewById<Slider>(R.id.sliderMinPay)
        val tvMin = view.findViewById<TextView>(R.id.tvMinPayValue)
        slider.value = current.minAmount.toFloat().coerceIn(0f, 500f)
        tvMin.text = "Desde S/ ${slider.value.toInt()}"
        slider.addOnChangeListener { _, value, _ -> tvMin.text = "Desde S/ ${value.toInt()}" }
        view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchNewest).isChecked =
            current.sortNewestFirst

        view.findViewById<View>(R.id.btnClearFilters).setOnClickListener {
            parentFragmentManager.setFragmentResult(REQUEST, bundleOf(EXTRA_CLEAR to true))
            dismiss()
        }
        view.findViewById<View>(R.id.btnApplyFilters).setOnClickListener {
            val cat = (0 until groupCat.childCount).map { groupCat.getChildAt(it) as Chip }
                .firstOrNull { it.isChecked }?.tag as? String ?: ""
            val dis = (0 until groupDis.childCount).map { groupDis.getChildAt(it) as Chip }
                .firstOrNull { it.isChecked }?.tag as? String ?: ""
            val newest = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchNewest).isChecked
            parentFragmentManager.setFragmentResult(
                REQUEST,
                bundleOf(
                    EXTRA_CLEAR to false,
                    EXTRA_CAT to cat,
                    EXTRA_DIS to dis,
                    EXTRA_MIN to slider.value.toDouble(),
                    EXTRA_SORT to newest
                )
            )
            dismiss()
        }
    }

    companion object {
        const val REQUEST = "publication_filters"
        const val EXTRA_CLEAR = "clear"
        const val EXTRA_CAT = "category"
        const val EXTRA_DIS = "district"
        const val EXTRA_MIN = "minAmount"
        const val EXTRA_SORT = "sortNewest"
        private const val ARG_CATS = "cats"
        private const val ARG_DIST = "districts"
        private const val ARG_CAT = "cur_cat"
        private const val ARG_DIS = "cur_dis"
        private const val ARG_MIN = "cur_min"
        private const val ARG_SORT = "cur_sort"

        fun newInstance(
            categories: List<String>,
            districts: List<String>,
            current: PublicationFilters
        ) = FilterBottomSheet().apply {
            arguments = bundleOf(
                ARG_CATS to ArrayList(categories),
                ARG_DIST to ArrayList(districts),
                ARG_CAT to current.category,
                ARG_DIS to current.district,
                ARG_MIN to current.minAmount,
                ARG_SORT to current.sortNewestFirst
            )
        }
    }
}
