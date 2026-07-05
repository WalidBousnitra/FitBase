package com.fitbase.util;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * A partir de targetSdk 35 Android fuerza edge-to-edge: el contenido se
 * dibuja detrás de la barra de estado/navegación si nadie gestiona los
 * insets. Sin esto, el layout se ve "desplazado" (sin margen arriba, hueco
 * abajo tapado por la barra de gestos).
 */
public final class InsetsHelper {

    private InsetsHelper() {}

    /** Aplica los insets del sistema como padding extra sobre el root de la Activity. */
    public static void aplicarInsetsSistema(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) return;
        View root = content.getChildAt(0);

        int padLeft = root.getPaddingLeft();
        int padTop = root.getPaddingTop();
        int padRight = root.getPaddingRight();
        int padBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(padLeft + barras.left, padTop + barras.top,
                    padRight + barras.right, padBottom + barras.bottom);
            return insets;
        });
    }
}
