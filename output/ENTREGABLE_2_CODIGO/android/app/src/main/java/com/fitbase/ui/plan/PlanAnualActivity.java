package com.fitbase.ui.plan;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fitbase.R;

/**
 * Pantalla del Plan Anual (Vista de 11 meses).
 * Muestra fases coloreadas, semana actual, checkpoints.
 * Referencia: REG-DEV-01 (ui.md) § 12
 */
public class PlanAnualActivity extends AppCompatActivity {

    private PlanAnualViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_anual);

        viewModel = new ViewModelProvider(this).get(PlanAnualViewModel.class);

        RecyclerView rvFases = findViewById(R.id.rvFases);
        rvFases.setLayoutManager(new LinearLayoutManager(this));

        TextView tvFaseActual = findViewById(R.id.tvFaseActual);
        TextView tvProgreso = findViewById(R.id.tvProgreso);

        // Observar datos
        viewModel.getPlanAnual().observe(this, plan -> {
            if (plan != null) {
                // Mostrar fase actual
                if (plan.faseActual != null && plan.faseActual.nombre != null) {
                    tvFaseActual.setText(String.format("FASE ACTUAL: %s", plan.faseActual.nombre));
                } else {
                    tvFaseActual.setText("PLAN ANUAL");
                }

                // Adapter de fases (null-safe)
                FaseAdapter adapter = new FaseAdapter(
                        plan.fases != null ? plan.fases : new java.util.ArrayList<>(),
                        plan.faseActual);
                rvFases.setAdapter(adapter);
            }
        });

        viewModel.cargarPlan();

        // Botón volver
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
    }
}
