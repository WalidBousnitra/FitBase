package com.fitbase.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.fitbase.R;

/**
 * Todas las Activities heredan de aquí para tener transiciones de pantalla
 * consistentes (deslizar+fundido) sin repetir overridePendingTransition en
 * cada startActivity/finish: al avanzar, la nueva entra desde la derecha y
 * la anterior se atenúa hacia la izquierda; al volver, al revés.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void startActivity(Intent intent, Bundle options) {
        super.startActivity(intent, options);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
