package com.fitbase.ui.plan;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fitbase.R;
import com.fitbase.data.model.Fase;

import java.util.List;

/**
 * Adapter para lista de fases del plan anual.
 * Colores según tipo de fase (Sistema_Diseno_Fitness.md).
 */
public class FaseAdapter extends RecyclerView.Adapter<FaseAdapter.FaseViewHolder> {

    private final List<Fase> fases;
    private final Fase faseActual;

    public FaseAdapter(List<Fase> fases, Fase faseActual) {
        this.fases = fases;
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
        holder.bind(fase, faseActual != null && fase.faseId.equals(faseActual.faseId));
    }

    @Override
    public int getItemCount() { return fases.size(); }

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
            tvNombre.setText(fase.nombre);
            tvFechas.setText(String.format("%s → %s (%d sem)", fase.fechaInicio, fase.fechaFin, fase.semanas));
            tvRir.setText(String.format("RIR %s | %s", fase.rirRango, fase.focoMuscular));

            // Color según tipo (Sistema_Diseno_Fitness.md)
            int color = getColorPorTipo(fase.tipo);
            indicadorColor.setBackgroundColor(color);

            // Destacar fase actual
            if (esActual) {
                cardFase.setAlpha(1.0f);
            } else {
                cardFase.setAlpha(0.6f);
            }
        }

        private int getColorPorTipo(String tipo) {
            if (tipo == null) return Color.parseColor("#5E5CE6");
            switch (tipo) {
                case "VOL": return Color.parseColor("#30D158");  // Verde - Volumen
                case "FZA": return Color.parseColor("#FF2D55");  // Rojo - Fuerza
                case "DEF": return Color.parseColor("#FF9500");  // Naranja - Definición
                case "MNT": return Color.parseColor("#5E5CE6");  // Morado - Mantenimiento
                case "DELOAD": return Color.parseColor("#FFD60A"); // Amarillo - Deload
                default: return Color.parseColor("#545458");
            }
        }
    }
}
