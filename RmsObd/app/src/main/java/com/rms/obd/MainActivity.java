package com.rms.obd;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressLint("MissingPermission")
public class MainActivity extends Activity {
    final Obd o = new Obd();
    final Handler h = new Handler(Looper.getMainLooper());
    final View[] pg = new View[4];
    final Button[] tb = new Button[4];
    TextView st, dtcTv, senTv;
    DashView dv;
    final Button[] mb = new Button[4];
    int mode, stState;
    ChartView ch;
    volatile boolean demo;

    int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    Button btn(String t) {
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        return b;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        android.content.SharedPreferences sp = getSharedPreferences("o", 0);
        o.cyl = sp.getInt("cyl", 1);
        o.injCc = sp.getInt("cc", 150);
        mode = sp.getInt("mode", 1);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1E1410);
        setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, ins) -> {
            Insets s = ins.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(s.left, s.top, s.right, s.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        LinearLayout hd = new LinearLayout(this);
        hd.setGravity(Gravity.CENTER_VERTICAL);
        hd.setPadding(dp(10), dp(6), dp(10), dp(6));
        ImageView lg = new ImageView(this);
        lg.setImageResource(R.drawable.logo);
        lg.setScaleType(ImageView.ScaleType.FIT_START);
        hd.addView(lg, new LinearLayout.LayoutParams(dp(120), dp(42)));
        hd.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        st = new TextView(this);
        st.setTextColor(0xFFFFFFFF);
        st.setTextSize(13);
        st.setTypeface(Typeface.DEFAULT_BOLD);
        st.setGravity(Gravity.CENTER);
        st.setPadding(dp(8), dp(6), dp(8), dp(6));
        setSt(0, "Bluetooth belum terhubung");
        Button bc = btn("Hubungkan"), bd = btn("Demo");
        bc.setOnClickListener(v -> connect());
        bd.setOnClickListener(v -> toggleDemo());
        hd.addView(bc);
        hd.addView(bd);
        root.addView(hd);
        root.addView(st, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout body = new FrameLayout(this);
        root.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        // Dasbor
        dv = new DashView(this);
        pg[0] = dv;
        // Grafik
        ch = new ChartView(this);
        LinearLayout cw = new LinearLayout(this);
        cw.setPadding(dp(8), dp(8), dp(8), dp(8));
        cw.addView(ch, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        pg[1] = cw;
        // Kode gangguan
        LinearLayout dl = new LinearLayout(this);
        dl.setOrientation(LinearLayout.VERTICAL);
        dl.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout dr = new LinearLayout(this);
        Button br = btn("Baca kode"), bx = btn("Hapus kode");
        br.setOnClickListener(v -> readDtc());
        bx.setOnClickListener(v -> clearDtc());
        dr.addView(br, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        dr.addView(bx, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        dl.addView(dr);
        dtcTv = new TextView(this);
        dtcTv.setTextColor(0xFFFFFFFF);
        dtcTv.setTextSize(18);
        dtcTv.setTypeface(Typeface.MONOSPACE);
        dtcTv.setText("Tekan \"Baca kode\" untuk memeriksa kode gangguan (check engine).");
        ScrollView sv = new ScrollView(this);
        sv.addView(dtcTv);
        dl.addView(sv);
        pg[2] = dl;
        // Data sensor
        senTv = new TextView(this);
        senTv.setTextColor(0xFFFFFFFF);
        senTv.setTextSize(18);
        senTv.setTypeface(Typeface.MONOSPACE);
        senTv.setPadding(dp(14), dp(14), dp(14), dp(14));
        ScrollView ss = new ScrollView(this);
        ss.addView(senTv);
        LinearLayout sl = new LinearLayout(this);
        sl.setOrientation(LinearLayout.VERTICAL);
        Button bi = btn("Atur injektor");
        bi.setOnClickListener(v -> injDialog());
        sl.addView(bi);
        LinearLayout mr = new LinearLayout(this);
        mr.setGravity(Gravity.CENTER_VERTICAL);
        mr.setPadding(dp(14), dp(4), dp(14), dp(4));
        TextView ml = new TextView(this);
        ml.setText("MODE");
        ml.setTextColor(0xFFFFFFFF);
        ml.setTextSize(18);
        ml.setTypeface(Typeface.DEFAULT_BOLD);
        mr.addView(ml, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        for (int i = 0; i < 4; i++) {
            final int k = i + 1;
            mb[i] = btn(String.valueOf(k));
            mb[i].setOnClickListener(v -> setMode(k));
            mr.addView(mb[i]);
        }
        updMode();
        sl.addView(mr);
        sl.addView(ss, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        pg[3] = sl;
        for (View v : pg) body.addView(v);

        LinearLayout tabs = new LinearLayout(this);
        String[] tn = {"Dasbor", "Grafik", "Kode", "Sensor"};
        for (int i = 0; i < 4; i++) {
            final int k = i;
            tb[i] = btn(tn[i]);
            tb[i].setOnClickListener(v -> show(k));
            tabs.addView(tb[i], new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        }
        root.addView(tabs);
        show(0);
        h.post(tick);
    }

    void show(int k) {
        for (int i = 0; i < 4; i++) {
            pg[i].setVisibility(i == k ? View.VISIBLE : View.GONE);
            tb[i].setAlpha(i == k ? 1f : .55f);
        }
    }

    final Runnable tick = new Runnable() {
        @Override
        public void run() {
            double tv = 0.5 + o.thr * 0.04, td = o.thr * 0.9;
            dv.set(o.rpm, o.spd, o.cool, tv, td, o.inj);
            ch.add((float) o.inj, (float) o.adv, (float) o.afr, (float) o.rpm);
            senTv.setText(String.format(Locale.US,
                    "MODE         %d\nRPM          %.0f\nKecepatan    %.0f km/j\nSuhu mesin   %.0f \u00B0C\nThrottle     %.2f V\nThrottle     %.0f \u00B0\nPengapian    %.1f \u00B0\nAFR          %.2f\nInjektor     %.2f ms\nTegangan     %.2f V",
                    mode, o.rpm, o.spd, o.cool, tv, td, o.adv, o.afr, o.inj, o.volt));
            if (stState == 2 && !o.ok) setSt(0, "Bluetooth terputus. Tekan Hubungkan untuk menyambung lagi");
            h.postDelayed(this, 250);
        }
    };

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    void connect() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 7);
            return;
        }
        BluetoothAdapter ad = ((BluetoothManager) getSystemService(BLUETOOTH_SERVICE)).getAdapter();
        if (ad == null || !ad.isEnabled()) { toast("Aktifkan Bluetooth dulu"); return; }
        final List<BluetoothDevice> ds = new ArrayList<>(ad.getBondedDevices());
        if (ds.isEmpty()) { toast("Pasangkan adaptor OBD-II lewat pengaturan Bluetooth ponsel dulu"); return; }
        String[] n = new String[ds.size()];
        for (int i = 0; i < n.length; i++) n[i] = ds.get(i).getName() + "\n" + ds.get(i).getAddress();
        new AlertDialog.Builder(this).setTitle("Pilih adaptor OBD-II").setItems(n, (d, i) -> go(ds.get(i))).show();
    }

    @Override
    public void onRequestPermissionsResult(int c, String[] p, int[] r) {
        super.onRequestPermissionsResult(c, p, r);
        if (r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) connect();
    }

    void go(final BluetoothDevice d) {
        demo = false;
        o.stop();
        setSt(1, "Menghubungkan ke adaptor...");
        new Thread(() -> {
            try {
                o.connect(d);
                o.start();
                h.post(() -> setSt(2, "Bluetooth terhubung: " + d.getName()));
            } catch (Exception e) {
                h.post(() -> setSt(0, "Gagal terhubung. Pastikan kontak menyala dan adaptor terpasang."));
            }
        }).start();
    }

    void toggleDemo() {
        if (demo) { demo = false; setSt(0, "Bluetooth belum terhubung"); return; }
        o.stop();
        demo = true;
        setSt(3, "Mode demo (data simulasi), bukan Bluetooth");
        new Thread(() -> {
            long s = System.currentTimeMillis();
            while (demo) {
                double t = (System.currentTimeMillis() - s) / 1000.0, k = (Math.sin(t / 3) + 1) / 2;
                o.rpm = 1200 + k * 8000;
                o.spd = k * 140;
                o.cool = 88 + k * 6;
                o.thr = k * 85;
                o.adv = 10 + k * 28;
                o.afr = 14.7 - k * 2.8 + Math.sin(t * 2) * .3;
                o.inj = 2 + k * 10;
                o.volt = 13.8;
                try { Thread.sleep(100); } catch (InterruptedException e) { }
            }
        }).start();
    }

    void setSt(int state, String text) {
        stState = state;
        int[] col = {0xFFB3261E, 0xFFB8860B, 0xFF1E7B34, 0xFF1565C0};
        st.setBackgroundColor(col[state]);
        st.setText("\u25CF " + text);
    }

    void setMode(int m) {
        mode = m;
        getSharedPreferences("o", 0).edit().putInt("mode", m).apply();
        updMode();
    }

    void updMode() {
        for (int i = 0; i < 4; i++) mb[i].setAlpha(mode == i + 1 ? 1f : .4f);
    }

    void injDialog() {
        final android.content.SharedPreferences sp = getSharedPreferences("o", 0);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(20), dp(8), dp(20), 0);
        final EditText c = new EditText(this), f = new EditText(this);
        c.setHint("Jumlah silinder");
        c.setText(String.valueOf(o.cyl));
        c.setInputType(InputType.TYPE_CLASS_NUMBER);
        f.setHint("Debit injektor (cc/menit)");
        f.setText(String.valueOf((int) o.injCc));
        f.setInputType(InputType.TYPE_CLASS_NUMBER);
        l.addView(c);
        l.addView(f);
        new AlertDialog.Builder(this).setTitle("Atur injektor").setView(l)
                .setPositiveButton("Simpan", (d, i) -> {
                    try {
                        o.cyl = Math.max(1, Integer.parseInt(c.getText().toString()));
                        o.injCc = Math.max(1, Integer.parseInt(f.getText().toString()));
                        sp.edit().putInt("cyl", o.cyl).putInt("cc", (int) o.injCc).apply();
                    } catch (Exception e) {
                        toast("Isi dengan angka yang valid");
                    }
                })
                .setNegativeButton("Batal", null).show();
    }

    void readDtc() {
        if (!o.ok) { dtcTv.setText("Hubungkan ke adaptor OBD-II dulu. Mode demo tidak membaca kode."); return; }
        dtcTv.setText("Membaca...");
        new Thread(() -> { String r = o.dtc(); h.post(() -> dtcTv.setText(r)); }).start();
    }

    void clearDtc() {
        if (!o.ok) { dtcTv.setText("Hubungkan ke adaptor OBD-II dulu."); return; }
        new AlertDialog.Builder(this).setTitle("Hapus kode gangguan?")
                .setMessage("Lampu check engine dan data freeze frame ikut terhapus.")
                .setPositiveButton("Hapus", (d, i) -> new Thread(() -> {
                    boolean ok = o.clear();
                    h.post(() -> dtcTv.setText(ok ? "Kode gangguan dihapus." : "Gagal menghapus kode."));
                }).start())
                .setNegativeButton("Batal", null).show();
    }
}
