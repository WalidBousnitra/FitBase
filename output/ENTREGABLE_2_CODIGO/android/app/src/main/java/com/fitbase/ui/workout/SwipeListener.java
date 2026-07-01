package com.fitbase.ui.workout;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Detector de swipes para la pantalla de entrenamiento.
 * Swipe izquierda = completar/siguiente
 * Swipe derecha = volver/cancelar
 * Referencia: REG-DEV-01 (ui.md) § 6
 */
public abstract class SwipeListener implements View.OnTouchListener {

    private final GestureDetector detector;
    private static final int UMBRAL_SWIPE = 100;
    private static final int UMBRAL_VELOCIDAD = 100;

    public SwipeListener(Context context) {
        detector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > UMBRAL_SWIPE && Math.abs(velocityX) > UMBRAL_VELOCIDAD) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return detector.onTouchEvent(event);
    }

    public abstract void onSwipeLeft();
    public abstract void onSwipeRight();
}
