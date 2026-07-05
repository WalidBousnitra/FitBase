package com.fitbase.ui.progression;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.fitbase.R;
import com.fitbase.data.model.MetricasProgresionResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Grafica minimalista de lineas: Peso, Grasa, Sueno y Pasos en un unico
 * lienzo, cada una normalizada 0-1 dentro de su propio rango (kg, %,
 * score 0-100 y pasos no son comparables en la misma escala real).
 * Sin ejes ni numeros — la leyenda de color vive en el XML de al lado.
 *
 * Las curvas se dibujan con interpolacion Catmull-Rom (suave entre los
 * puntos reales, sin inventar ni desplazar ningun valor) porque con datos
 * ruidosos día a día unir los puntos con lineas rectas se lee como rayajos
 * ilegibles — la curva pasa exactamente por cada dato, solo cambia cómo se
 * conectan visualmente.
 */
public class ProgresionChartView extends View {

    private static final int[] COLOR_RES = {
            R.color.colorAccentPrimary,   // peso
            R.color.colorGrasa,           // grasa
            R.color.colorChartSecondary,  // sueno
            R.color.colorSuccess,         // pasos
    };

    private interface Extractor {
        Float valor(MetricasProgresionResponse.ZeppEntry e);
    }

    private static final Extractor[] EXTRACTORES = {
            e -> e.pesoKg,
            e -> e.grasaPct,
            e -> e.sleepScore != null ? (float) e.sleepScore : null,
            e -> (float) e.pasos,
    };

    private final List<float[]> series = new ArrayList<>();
    private final Paint lineaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint puntoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] colores = new int[COLOR_RES.length];
    private int colorFondo;

    public ProgresionChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        lineaPaint.setStyle(Paint.Style.STROKE);
        lineaPaint.setStrokeWidth(dp(2f));
        lineaPaint.setStrokeJoin(Paint.Join.ROUND);
        lineaPaint.setStrokeCap(Paint.Cap.ROUND);

        gridPaint.setColor(ContextCompat.getColor(context, R.color.colorSeparator));
        gridPaint.setStrokeWidth(dp(1f));

        for (int i = 0; i < COLOR_RES.length; i++) {
            colores[i] = ContextCompat.getColor(context, COLOR_RES[i]);
        }
        colorFondo = ContextCompat.getColor(context, R.color.colorBackground);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public void setDatos(List<MetricasProgresionResponse.ZeppEntry> zepp) {
        series.clear();
        if (zepp != null && !zepp.isEmpty()) {
            for (Extractor ex : EXTRACTORES) {
                float[] valores = new float[zepp.size()];
                for (int i = 0; i < zepp.size(); i++) {
                    Float v = ex.valor(zepp.get(i));
                    valores[i] = v != null ? v : Float.NaN;
                }
                series.add(valores);
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (series.isEmpty()) return;

        int w = getWidth();
        int h = getHeight();
        float padY = dp(12);

        // Lineas guia sutiles (25/50/75% de la altura), sin numeros.
        for (int i = 1; i < 4; i++) {
            float y = padY + (h - 2 * padY) * i / 4f;
            canvas.drawLine(0, y, w, y, gridPaint);
        }

        for (int s = 0; s < series.size(); s++) {
            float[] valores = series.get(s);
            float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
            for (float v : valores) {
                if (Float.isNaN(v)) continue;
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
            float rango = (max - min) > 0.0001f ? (max - min) : 1f;

            // Puntos validos agrupados en tramos continuos (un hueco = sin
            // pesada ese dia; no se interpola a traves del hueco).
            List<List<float[]>> tramos = new ArrayList<>();
            List<float[]> tramoActual = null;
            for (int i = 0; i < valores.length; i++) {
                if (Float.isNaN(valores[i])) {
                    tramoActual = null;
                    continue;
                }
                float x = valores.length > 1 ? w * i / (float) (valores.length - 1) : w / 2f;
                float norm = (valores[i] - min) / rango;
                float y = padY + (h - 2 * padY) * (1f - norm);
                if (tramoActual == null) {
                    tramoActual = new ArrayList<>();
                    tramos.add(tramoActual);
                }
                tramoActual.add(new float[]{x, y});
            }
            if (tramos.isEmpty()) continue;

            lineaPaint.setColor(colores[s]);
            float[] ultimoPunto = null;
            for (List<float[]> tramo : tramos) {
                if (tramo.size() >= 2) {
                    canvas.drawPath(suavizar(tramo), lineaPaint);
                }
                ultimoPunto = tramo.get(tramo.size() - 1);
            }

            // Punto final: ancla la identidad de la serie en el extremo
            // derecho con un halo del color de fondo, para que se distinga
            // aunque cruce por encima de otra linea (marks-and-anatomy:
            // "surface ring" en vez de un borde dibujado).
            if (ultimoPunto != null) {
                puntoPaint.setColor(colorFondo);
                canvas.drawCircle(ultimoPunto[0], ultimoPunto[1], dp(5), puntoPaint);
                puntoPaint.setColor(colores[s]);
                canvas.drawCircle(ultimoPunto[0], ultimoPunto[1], dp(3.5f), puntoPaint);
            }
        }
    }

    /**
     * Catmull-Rom → Bezier (tension uniforme, factor 1/6): la curva pasa
     * exactamente por cada punto real, solo se suaviza cómo se conectan.
     */
    private Path suavizar(List<float[]> pts) {
        Path path = new Path();
        path.moveTo(pts.get(0)[0], pts.get(0)[1]);
        int n = pts.size();
        for (int i = 0; i < n - 1; i++) {
            float[] p0 = pts.get(Math.max(i - 1, 0));
            float[] p1 = pts.get(i);
            float[] p2 = pts.get(i + 1);
            float[] p3 = pts.get(Math.min(i + 2, n - 1));

            float c1x = p1[0] + (p2[0] - p0[0]) / 6f;
            float c1y = p1[1] + (p2[1] - p0[1]) / 6f;
            float c2x = p2[0] - (p3[0] - p1[0]) / 6f;
            float c2y = p2[1] - (p3[1] - p1[1]) / 6f;

            path.cubicTo(c1x, c1y, c2x, c2y, p2[0], p2[1]);
        }
        return path;
    }
}
