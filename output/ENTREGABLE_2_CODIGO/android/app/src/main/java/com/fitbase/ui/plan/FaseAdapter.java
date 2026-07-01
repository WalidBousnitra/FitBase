package com.fitbase.ui.plan;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
 * Colores según tipo de fase (Sistema_Diseno_Fitness.md).
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

        FaseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvFaseNombre);
            tvFechas = itemView.findViewById(R.id.tvFaseFechas);
            tvRir = itemView.findViewById(R.id.tvFaseRir);
            indicadorColor = itemView.findViewById(R.id.indicadorColor);
            cardFase = itemView.findViewById(R.id.cardFase);
        }

        void bind(Fase fase, boolean esActual) {
            tvNombre.setText(fase.nombre != null ? fase.nombre : "—");

            // Format dates: "31 ago 2026 → 27 sep 2026 · 4 sem"
            String inicio = formatearFecha(fase.fechaInicio);
            String fin = formatearFecha(fase.fechaFin);
            tvFechas.setText(String.format("%s → %s · %d sem", inicio, fin, fase.semanas));

            tvRir.setText(String.format("RIR %s · %s",
                    fase.rirRango != null ? fase.rirRango : "—",
                    fase.focoMuscular != null ? fase.focoMuscular : "—"));

            // Color según tipo
            int color = getColorPorTipo(fase.tipo);
            indicadorColor.setBackgroundColor(color);

            // Destacar fase actual: fondo con el color de su tipo (suave) + negrita
            if (esActual) {
                cardFase.setAlpha(1.0f);
                // Fondo = color del tipo con 25% opacidad
                int bgColor = Color.argb(64, Color.red(color), Color.green(color), Color.blue(color));
                cardFase.setBackgroundColor(bgColor);
                indicadorColor.getLayoutParams().width = 8; // Barra más gruesa
                indicadorColor.requestLayout();
                tvNombre.setTypeface(null, Typeface.BOLD);
                tvNombre.setText("▶ " + (fase.nombre != null ? fase.nombre : "—"));
                tvNombre.setTextColor(color);
            } else {
                cardFase.setAlpha(1.0f);
                cardFase.setBackgroundColor(Color.parseColor("#FAFAFA"));
                indicadorColor.getLayoutParams().width = 4;
                indicadorColor.requestLayout();
                tvNombre.setTypeface(null, Typeface.NORMAL);
                tvNombre.setTextColor(Color.parseColor("#212121"));
            }
        }

        /** Convierte cualquier formato de fecha a "dd MMM yyyy" limpio */
        private String formatearFecha(String fecha) {
            if (fecha == null) return "?";

            // Strip any time portion ("T..." or " HH:mm:ss")
            if (fecha.contains("T")) {
                fecha = fecha.substring(0, fecha.indexOf("T"));
            } else if (fecha.length() > 10) {
                fecha = fecha.substring(0, 10);
            }

            // Try yyyy-MM-dd
            try {
                SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date d = isoFmt.parse(fecha);
                if (d != null) {
                    return new SimpleDateFormat("d MMM yy", new Locale("es", "ES")).format(d);
                }
            } catch (ParseException ignored) {}

            // Try dd/MM/yyyy or dd/MM
            try {
                SimpleDateFormat dmFmt = new SimpleDateFormat("dd/MM", Locale.getDefault());
                Date d = dmFmt.parse(fecha);
                if (d != null) return fecha; // Already short
            } catch (ParseException ignored) {}

            return fecha;
        }

        private int getColorPorTipo(String tipo) {
            if (tipo == null) return Color.parseColor("#9E9E9E");
            switch (tipo) {
                case "VOL": return Color.parseColor("#5C8A5C");  // Verde muted - Volumen
                case "FZA": return Color.parseColor("#FF5722");  // Naranja accent - Fuerza
                case "DEF": return Color.parseColor("#C4930A");  // Ámbar muted - Definición
                case "MNT": return Color.parseColor("#5A8A9E");  // Gris azulado - Mantenimiento
                case "DELOAD": return Color.parseColor("#9E9E9E"); // Gris neutro - Deload
                default: return Color.parseColor("#6B6B6B");
            }
        }
    }
}
