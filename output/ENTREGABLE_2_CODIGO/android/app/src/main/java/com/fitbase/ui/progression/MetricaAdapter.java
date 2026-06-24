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
 * Soporta 3 tipos: PESO, SUENO, VOLUMEN.
 */
public class MetricaAdapter extends RecyclerView.Adapter<MetricaAdapter.ViewHolder> {

    public static final int TIPO_PESO = 0;
    public static final int TIPO_SUENO = 1;
    public static final int TIPO_VOLUMEN = 2;

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
            case TIPO_PESO:
                MetricasProgresionResponse.PesoEntry peso =
                        (MetricasProgresionResponse.PesoEntry) items.get(position);
                holder.tvFecha.setText(peso.fecha);
                holder.tvValorPrincipal.setText(String.format("%.1f kg", peso.pesoKg));
                if (peso.grasaPct != null) {
                    holder.tvValorSecundario.setText(String.format("%.1f%% grasa", peso.grasaPct));
                    holder.tvValorSecundario.setVisibility(View.VISIBLE);
                } else {
                    holder.tvValorSecundario.setVisibility(View.GONE);
                }
                break;

            case TIPO_SUENO:
                MetricasProgresionResponse.ZeppEntry zepp =
                        (MetricasProgresionResponse.ZeppEntry) items.get(position);
                holder.tvFecha.setText(zepp.fecha);
                holder.tvValorPrincipal.setText(String.format("Score: %d", zepp.sleepScore));
                holder.tvValorSecundario.setText(String.format("%.1fh | FC %d | HRV %d",
                        zepp.sleepHoras, zepp.hrReposo, zepp.hrvRmssd));
                holder.tvValorSecundario.setVisibility(View.VISIBLE);
                break;

            case TIPO_VOLUMEN:
                MetricasProgresionResponse.VolumenEntry vol =
                        (MetricasProgresionResponse.VolumenEntry) items.get(position);
                holder.tvFecha.setText(vol.fecha);
                holder.tvValorPrincipal.setText(String.format("%,d kg", vol.volumenKg));
                holder.tvValorSecundario.setVisibility(View.GONE);
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
