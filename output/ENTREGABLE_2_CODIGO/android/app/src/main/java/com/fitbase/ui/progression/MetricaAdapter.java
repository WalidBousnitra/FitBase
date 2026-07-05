package com.fitbase.ui.progression;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fitbase.R;
import com.fitbase.data.model.MetricasProgresionResponse;

import java.util.List;

/**
 * Adapter generico para mostrar filas de metricas en la pantalla de progresion.
 * Peso, Grasa, Sueno y Pasos ahora se muestran en ProgresionChartView (una
 * sola grafica); este adapter solo cubre SUBJETIVA (energia/estres/notas).
 */
public class MetricaAdapter extends RecyclerView.Adapter<MetricaAdapter.ViewHolder> {

    public static final int TIPO_SUBJETIVA = 3;

    private final List<?> items;
    private final int tipo;

    public MetricaAdapter(List<?> items, int tipo) {
        this.items = items;
        this.tipo = tipo;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_metrica, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        switch (tipo) {
            case TIPO_SUBJETIVA:
                MetricasProgresionResponse.SubjetivaEntry subj =
                        (MetricasProgresionResponse.SubjetivaEntry) items.get(position);
                holder.tvFecha.setText(subj.fecha);
                holder.tvValorPrincipal.setText(subj.energia != null
                        ? String.format("Energía %d/5", subj.energia) : "Energía —");
                String estres = subj.estres != null ? String.format("Estrés %d/5", subj.estres) : "Estrés —";
                holder.tvValorSecundario.setText(
                        subj.notas != null && !subj.notas.isEmpty() ? estres + " · " + subj.notas : estres);
                holder.tvValorSecundario.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFecha, tvValorPrincipal, tvValorSecundario;

        ViewHolder(View itemView) {
            super(itemView);
            tvFecha = itemView.findViewById(R.id.tvMetricaFecha);
            tvValorPrincipal = itemView.findViewById(R.id.tvMetricaValor);
            tvValorSecundario = itemView.findViewById(R.id.tvMetricaSecundario);
        }
    }
}
