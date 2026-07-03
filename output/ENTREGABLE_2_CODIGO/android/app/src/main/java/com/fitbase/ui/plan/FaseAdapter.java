package com.fitbase.ui.plan;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fitbase.R;
import com.fitbase.data.model.Fase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter para lista de fases del plan anual.
 * Colores según tipo de fase. Fase actual destacada con borde naranja y badge.
 */
public class FaseAdapter extends RecyclerView.Adapter<FaseAdapter.FaseViewHolder> {

    private final List<Fase> fases;
    private final Fase faseActual;

    public FaseAdapter(List<Fase> fases, Fase faseActual) {
        this.fases = fases != null ? fases : new java.util.ArrayList<>();
        this.faseActual = faseActual;
    }

    @NonNull
    @Override
    public FaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_fase, parent, false);
        return new FaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FaseViewHolder holder, int position) {
        Fase fase = fases.get(position);
        boolean esActual = faseActual != null && fase.faseId != null
                && fase.faseId.equals(faseActual.faseId);
        holder.bind(fase, esActual);
    }

    @Override
    public int getItemCount() { return fases != null ? fases.size() : 0; }

    static class FaseViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNombre;
        private final TextView tvFechas;
        private final TextView tvRir;
        private final View indicadorColor;
        private final View cardFase;
        private final TextView badgeActual;

        FaseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvFaseNombre);
            tvFechas = itemView.findViewById(R.id.tvFaseFechas);
            tvRir = itemView.findViewById(R.id.tvFaseRir);
            indicadorColor = itemView.findViewById(R.id.indicadorColor);
            cardFase = itemView.findViewById(R.id.cardFase);
            badgeActual = itemView.findViewById(R.id.badgeActual);
        }

        void bind(Fase fase, boolean esActual) {
            tvNombre.setText(fase.nombre != null ? fase.nombre : "—");

            // Format dates: "31 ago 2026 → 27 sep 2026 · 4 sem"
            String inicio = formatearFecha(fase.fechaInicio);
            String fin = formatearFecha(fase.fechaFin);
            tvFechas.setText(String.format("%s → %s · %d sem", inicio, fin, fase.semanas));

            // RIR - solo mostrar si NO parece una fecha
            String rirTexto = fase.rirRango;
            if (rirTexto != null && looksLikeDate(rirTexto)) {
                rirTexto = "—";
            }
            String focoTexto = fase.focoMuscular;
            if (focoTexto != null && looksLikeDate(focoTexto)) {
                focoTexto = "—";
            }
            tvRir.setText(String.format("RIR %s · %s",
                    rirTexto != null ? rirTexto : "—",
                    focoTexto != null ? focoTexto : "—"));

            // Color según tipo
            int color = getColorPorTipo(fase.tipo);
            indicadorColor.setBackgroundColor(color);

            // Destacar fase actual
            if (esActual) {
                cardFase.setBackgroundResource(R.drawable.bg_fase_actual);
                indicadorColor.getLayoutParams().width = dpToPx(6);
                indicadorColor.requestLayout();
                tvNombre.setTypeface(null, Typeface.BOLD);
                tvNombre.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.colorAccentPrimary));
                badgeActual.setVisibility(View.VISIBLE);
            } else {
                cardFase.setBackgroundResource(R.drawable.bg_card_flat);
                indicadorColor.getLayoutParams().width = dpToPx(5);
                indicadorColor.requestLayout();
                tvNombre.setTypeface(null, Typeface.NORMAL);
                tvNombre.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.colorTextPrimary));
                badgeActual.setVisibility(View.GONE);
            }
        }

        /**
         * Extrae SOLO la fecha (sin hora/timezone) de cualquier formato posible:
         * - "2026-04-02T00:00:00.000Z"
         * - "2026-04-02 00:00:00"
         * - "Wed Apr 02 2026 00:00:00 GMT+0200"
         * - "2026-04-02"
         * - "04/02/2026"
         * Lo devuelve como "2 abr 2026" (legible en español).
         */
        private String formatearFecha(String fecha) {
            if (fecha == null || fecha.isEmpty()) return "?";

            // Strip time portion con "T"
            if (fecha.contains("T")) {
                fecha = fecha.substring(0, fecha.indexOf("T"));
            }
            // Strip time portion con espacio (pero no si es solo fecha corta)
            if (fecha.length() > 10 && fecha.contains(" ")) {
                fecha = fecha.substring(0, 10);
            }
            // Remove trailing whitespace
            fecha = fecha.trim();

            // Try yyyy-MM-dd → "d MMM yyyy"
            try {
                SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date d = isoFmt.parse(fecha);
                if (d != null) {
                    return new SimpleDateFormat("d MMM yy", new Locale("es", "ES")).format(d);
                }
            } catch (ParseException ignored) {}

            // Try dd/MM/yyyy
            try {
                SimpleDateFormat dmyFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                Date d = dmyFmt.parse(fecha);
                if (d != null) {
                    return new SimpleDateFormat("d MMM yy", new Locale("es", "ES")).format(d);
                }
            } catch (ParseException ignored) {}

            // Fallback: if it still looks like a date with numbers, just return first 10 chars
            if (fecha.length() > 10) {
                return fecha.substring(0, 10);
            }
            return fecha;
        }

        /** Detecta si un string parece una fecha (para limpiar RIR campo contaminado) */
        private boolean looksLikeDate(String s) {
            if (s == null) return false;
            return s.matches(".*\\d{4}-\\d{2}-\\d{2}.*")
                    || s.matches(".*\\d{2}/\\d{2}/\\d{4}.*")
                    || s.contains("GMT")
                    || s.contains("T00:");
        }

        private int getColorPorTipo(String tipo) {
            if (tipo == null) return Color.parseColor("#78909C");
            switch (tipo) {
                case "VOL": return Color.parseColor("#4CAF50");  // Verde vibrante - Volumen
                case "FZA": return Color.parseColor("#FF5722");  // Naranja accent - Fuerza
                case "DEF": return Color.parseColor("#FFB300");  // Ámbar brillante - Definición
                case "MNT": return Color.parseColor("#42A5F5");  // Azul cielo - Mantenimiento
                case "DELOAD": return Color.parseColor("#78909C"); // Gris azulado - Deload
                default: return Color.parseColor("#78909C");
            }
        }

        private int dpToPx(int dp) {
            return (int) (dp * itemView.getContext().getResources().getDisplayMetrics().density);
        }
    }
}
