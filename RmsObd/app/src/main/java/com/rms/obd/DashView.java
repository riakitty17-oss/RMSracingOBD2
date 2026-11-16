package com.rms.obd;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import java.util.Locale;

/** Dasbor bergaya spedometer digital: bar RPM, kecepatan besar, dan empat panel angka. */
public class DashView extends View {
    private double rpm, spd, cool, tv, td, inj;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Typeface big = Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC);

    public DashView(Context c) { super(c); }

    public void set(double rpm, double spd, double cool, double tv, double td, double inj) {
        this.rpm = rpm; this.spd = spd; this.cool = cool; this.tv = tv; this.td = td; this.inj = inj;
        invalidate();
    }

    private static String fmt(double v, int d) { return String.format(Locale.US, "%." + d + "f", v); }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), m = w / 30f;
        // Bar RPM bersegmen: hijau, kuning, merah
        int n = 30;
        float sw = (w - 2 * m) / n, bh = h * .07f, by = m;
        int lit = Math.round((float) Math.min(1, rpm / 12000.0) * n);
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < n; i++) {
            int col = i < 18 ? 0xFF3DDC84 : (i < 24 ? 0xFFF5A623 : 0xFFE53935);
            p.setColor(i < lit ? col : 0xFF3A2A22);
            float x = m + i * sw;
            c.drawRoundRect(new RectF(x + 1, by, x + sw - 1, by + bh), 3, 3, p);
        }
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextAlign(Paint.Align.RIGHT);
        p.setColor(0xFFFFFFFF);
        p.setTextSize(h * .045f);
        c.drawText(fmt(rpm, 0) + " RPM", w - m, by + bh + h * .055f, p);
        // Kecepatan besar di tengah
        p.setTypeface(big);
        p.setTextAlign(Paint.Align.CENTER);
        float ts = h * .34f;
        p.setTextSize(ts);
        float mw = p.measureText("188");
        if (mw > w * .85f) ts *= w * .85f / mw;
        p.setTextSize(ts);
        p.setColor(0xFFFFFFFF);
        float cy = h * .52f;
        c.drawText(String.valueOf(Math.round(spd)), w / 2, cy, p);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(h * .05f);
        p.setColor(0xFFF5A623);
        c.drawText("km/j", w / 2, cy + h * .06f, p);
        // Empat panel digital
        String[] lb = {"SUHU", "THROTTLE", "THROTTLE", "INJEKTOR"};
        String[] vl = {fmt(cool, 0), fmt(tv, 2), fmt(td, 0), fmt(inj, 1)};
        String[] un = {"\u00B0C", "V", "\u00B0", "ms"};
        float ty = h * .64f, th = (h - ty - m) / 2 - m / 2, tw = (w - 3 * m) / 2;
        for (int i = 0; i < 4; i++) {
            float x = m + (i % 2) * (tw + m), y = ty + (i / 2) * (th + m);
            RectF r = new RectF(x, y, x + tw, y + th);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFF2A1D17);
            c.drawRoundRect(r, m, m, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2);
            p.setColor(0xFF4A362B);
            c.drawRoundRect(r, m, m, p);
            p.setStyle(Paint.Style.FILL);
            p.setTextAlign(Paint.Align.LEFT);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setColor(0xFFBBAA99);
            p.setTextSize(th * .2f);
            c.drawText(lb[i], x + m * .7f, y + th * .3f, p);
            p.setTypeface(big);
            p.setColor(i == 0 && cool > 105 ? 0xFFE53935 : 0xFFF5A623);
            p.setTextSize(th * .5f);
            c.drawText(vl[i], x + m * .7f, y + th * .85f, p);
            float vw = p.measureText(vl[i]);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextSize(th * .2f);
            p.setColor(0xFFBBAA99);
            c.drawText(un[i], x + m * .7f + vw + m * .4f, y + th * .85f, p);
        }
    }
}
