package com.rms.obd;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;
import java.util.Locale;

/** Diagram: biru = injektor, merah = pengapian, kuning = AFR, hijau tua = RPM. */
public class ChartView extends View {
    static final int N = 160;
    private final float[][] d = new float[4][N];
    private final float[] cur = new float[4];
    private final int[] col = {0xFF1E5BFF, 0xFFE53935, 0xFFFFC400, 0xFF1B5E20};
    private final String[] nm = {"INJEKTOR", "Pengapian", "AFR", "RPM"};
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ChartView(Context c) { super(c); }

    private static float n(float x) { return Math.max(0, Math.min(1, x)); }

    public void add(float fuel, float adv, float afr, float rpm) {
        float[] v = {n(fuel / 20), n((adv + 10) / 60), n((afr - 8) / 12), n(rpm / 12000)};
        cur[0] = fuel; cur[1] = adv; cur[2] = afr; cur[3] = rpm;
        for (int i = 0; i < 4; i++) {
            System.arraycopy(d[i], 1, d[i], 0, N - 1);
            d[i][N - 1] = v[i];
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), ts = w / 26f, top = ts * 4.2f, bot = ts * 2f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFECEFF1);
        c.drawRoundRect(new RectF(0, 0, w, h), ts, ts, p);
        String[] val = {String.format(Locale.US, "%.1f ms", cur[0]), String.format(Locale.US, "%.1f\u00B0", cur[1]),
                String.format(Locale.US, "%.1f", cur[2]), String.format(Locale.US, "%.0f rpm", cur[3])};
        p.setTextSize(ts * 1.0f);
        p.setTextAlign(Paint.Align.LEFT);
        for (int i = 0; i < 4; i++) {
            float x = ts + (i % 2) * w / 2, y = ts * 1.8f + (i / 2) * ts * 1.7f;
            p.setColor(col[i]);
            c.drawRoundRect(new RectF(x, y - ts * .8f, x + ts, y + ts * .1f), 4, 4, p);
            p.setColor(0xFF263238);
            c.drawText(nm[i] + " " + val[i], x + ts * 1.4f, y, p);
        }
        float l = ts, r = w - ts, t = top, b = h - bot;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2);
        p.setColor(0xFFB0BEC5);
        for (int i = 0; i <= 4; i++) c.drawLine(l, t + (b - t) * i / 4, r, t + (b - t) * i / 4, p);
        p.setStrokeWidth(ts / 4);
        p.setStrokeJoin(Paint.Join.ROUND);
        for (int s = 0; s < 4; s++) {
            Path pa = new Path();
            for (int i = 0; i < N; i++) {
                float x = l + (r - l) * i / (N - 1), y = b - (b - t) * d[s][i];
                if (i == 0) pa.moveTo(x, y); else pa.lineTo(x, y);
            }
            p.setColor(col[s]);
            c.drawPath(pa, p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF607D8B);
        p.setTextSize(ts * .8f);
        c.drawText("Skala: Injektor 0-20 ms | Pengapian -10..50\u00B0 | AFR 8-20 | RPM 0-12000", l, h - ts * .6f, p);
    }
}
