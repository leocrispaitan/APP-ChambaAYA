package com.proyecto.chambaya.ui.jobs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.proyecto.chambaya.R
import kotlin.math.abs

/**
 * Carrusel del home: 2 tarjetas fijas (Empleos / Tiempo libre).
 *
 * El banner YA NO cambia con el toggle de [R.id.feedModeTabs]: ambas tarjetas
 * viven en el carrusel y rotan solas (más swipe manual). El CTA de cada tarjeta
 * activa su modo del feed ([onCtaClick] con true = Tiempo libre).
 * El número grande ([count]) se actualiza con datos reales del feed vía
 * [actualizarConteos]; solo repinta si el valor cambió.
 */
data class BannerPromo(
    val esTiempoLibre: Boolean,
    val badge: String,
    val titulo: String,
    val subtitulo: String,
    val textoCta: String,
    val legal: String,
    var count: Int = 0
)

/**
 * Efecto carrusel profesional: separación visible entre tarjetas durante el
 * desplazamiento + escala sutil en las páginas laterales. Sin esto las
 * tarjetas van pegadas edge-to-edge y el swipe se ve mal.
 */
class BannerCarouselTransformer(marginPx: Int, private val minScale: Float = 0.93f) :
    ViewPager2.PageTransformer {
    private val margin = MarginPageTransformer(marginPx)

    override fun transformPage(page: View, position: Float) {
        margin.transformPage(page, position)
        val scale = (minScale + (1f - minScale) * (1f - abs(position))).coerceIn(minScale, 1f)
        page.scaleX = scale
        page.scaleY = scale
        page.alpha = 0.65f + 0.35f * (1f - abs(position)).coerceIn(0f, 1f)
    }
}
class BannerPromoAdapter(
    private val onCtaClick: (esTiempoLibre: Boolean) -> Unit = {}
) : RecyclerView.Adapter<BannerPromoAdapter.VH>() {

    private val items = mutableListOf<BannerPromo>()

    fun submitList(nueva: List<BannerPromo>) {
        items.clear()
        items.addAll(nueva)
        notifyDataSetChanged()
    }

    /** Actualiza los contadores sin parpadeo si no cambiaron. */
    fun actualizarConteos(empleos: Int, tiempoLibre: Int) {
        items.forEachIndexed { i, b ->
            val nuevo = if (b.esTiempoLibre) tiempoLibre.coerceAtLeast(0) else empleos.coerceAtLeast(0)
            if (b.count != nuevo) {
                b.count = nuevo
                notifyItemChanged(i)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_banner_promo, parent, false)
        return VH(v, onCtaClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class VH(view: View, private val onCtaClick: (Boolean) -> Unit) : RecyclerView.ViewHolder(view) {
        private val ivBackground: ImageView = view.findViewById(R.id.ivBannerBackground)
        private val tvBadge: TextView = view.findViewById(R.id.tvBannerBadge)
        private val tvTitle: TextView = view.findViewById(R.id.tvBannerTitle)
        private val tvSubtitle: TextView = view.findViewById(R.id.tvBannerSubtitle)
        private val tvCount: TextView = view.findViewById(R.id.tvBannerCount)
        private val tvLegal: TextView = view.findViewById(R.id.tvBannerLegal)
        private val btnCta: Button = view.findViewById(R.id.btnBannerCta)

        fun bind(b: BannerPromo) {
            ivBackground.setImageResource(
                if (b.esTiempoLibre) R.drawable.tarjeta_tiempo_librefondo
                else R.drawable.tarjeta_empleosfondo
            )
            tvBadge.text = b.badge
            tvTitle.text = b.titulo
            tvSubtitle.text = b.subtitulo
            tvCount.text = b.count.coerceAtLeast(0).toString()
            tvLegal.text = b.legal
            btnCta.text = b.textoCta
            btnCta.setOnClickListener { onCtaClick(b.esTiempoLibre) }
        }
    }
}
