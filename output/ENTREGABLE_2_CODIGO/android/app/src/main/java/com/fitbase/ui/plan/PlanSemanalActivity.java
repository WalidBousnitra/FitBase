package com.fitbase.ui.plan;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.fitbase.R;

/**
 * Pantalla Plan Semanal (microciclo).
 * Referencia: REG-DEV-01 (ui.md) § 13
 */
public class PlanSemanalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_semanal);
        // TODO: Implementar vista semanal con ejercicios por día
    }
}
