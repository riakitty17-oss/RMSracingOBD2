package com.rms.obd;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.UUID;

/** Koneksi ELM327 (Bluetooth SPP) dan pembacaan PID standar OBD-II. */
@SuppressLint("MissingPermission")
public class Obd {
    static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public volatile double rpm, spd, cool, thr, adv, afr, fuel, volt, inj;
    public volatile int cyl = 1;
    public volatile double injCc = 150;
    public volatile boolean ok;
    private volatile boolean run;
    private BluetoothSocket sk;
    private InputStream in;
    private OutputStream out;

    public void connect(BluetoothDevice d) throws IOException {
        sk = d.createRfcommSocketToServiceRecord(SPP);
        sk.connect();
        in = sk.getInputStream();
        out = sk.getOutputStream();
        for (String c : new String[]{"ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0"}) q(c);
        ok = true;
    }

    synchronized String q(String c) throws IOException {
        out.write((c + "\r").getBytes());
        out.flush();
        StringBuilder b = new StringBuilder();
        long t = System.currentTimeMillis();
        while (System.currentTimeMillis() - t < 4000) {
            if (in.available() > 0) {
                int ch = in.read();
                if (ch == '>') break;
                b.append((char) ch);
            } else {
                try { Thread.sleep(10); } catch (InterruptedException e) { }
            }
        }
        return b.toString().replaceAll("\\s", "").replace("SEARCHING...", "");
    }

    private int[] p(String pid) {
        try {
            String r = q("01" + pid), k = "41" + pid;
            int i = r.indexOf(k);
            if (i < 0) return null;
            r = r.substring(i + 4);
            int[] a = new int[r.length() / 2];
            for (int j = 0; j < a.length; j++) a[j] = Integer.parseInt(r.substring(2 * j, 2 * j + 2), 16);
            return a;
        } catch (IOException e) {
            ok = false;
            return null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public void start() {
        run = true;
        new Thread(() -> {
            while (run && ok) {
                int[] a;
                if ((a = p("0C")) != null && a.length > 1) rpm = (a[0] * 256 + a[1]) / 4.0;
                if ((a = p("0D")) != null && a.length > 0) spd = a[0];
                if ((a = p("05")) != null && a.length > 0) cool = a[0] - 40;
                if ((a = p("11")) != null && a.length > 0) thr = a[0] * 100 / 255.0;
                if ((a = p("0E")) != null && a.length > 0) adv = a[0] / 2.0 - 64;
                if ((a = p("24")) != null && a.length > 1) afr = (a[0] * 256 + a[1]) * 2.0 / 65536 * 14.7;
                if ((a = p("5E")) != null && a.length > 1) fuel = (a[0] * 256 + a[1]) / 20.0;
                else if ((a = p("10")) != null && a.length > 1) fuel = (a[0] * 256 + a[1]) / 100.0 * 0.3287;
                if ((a = p("42")) != null && a.length > 1) volt = (a[0] * 256 + a[1]) / 1000.0;
                // Estimasi durasi injektor terbuka (ms) dari laju bahan bakar, RPM, jumlah silinder, dan debit injektor.
                double cc = fuel * 1000 / 60.0;
                inj = rpm > 300 && injCc > 0 ? cc / (cyl * rpm / 2.0) / injCc * 60000 : 0;
            }
        }).start();
    }

    public void stop() {
        run = false;
        ok = false;
        try { if (sk != null) sk.close(); } catch (IOException e) { }
    }

    public String dtc() {
        try {
            String r = q("03");
            int i = r.indexOf("43");
            if (i < 0) return "Tidak ada respons dari ECU";
            r = r.substring(i + 2);
            if ((r.length() / 2) % 2 == 1) r = r.substring(2);
            StringBuilder s = new StringBuilder();
            for (int j = 0; j + 4 <= r.length(); j += 4) {
                int v = Integer.parseInt(r.substring(j, j + 4), 16);
                if (v == 0) continue;
                s.append("PCBU".charAt(v >> 14)).append(String.format(Locale.US, "%d%03X", (v >> 12) & 3, v & 0xFFF)).append("\n");
            }
            return s.length() == 0 ? "Tidak ada kode gangguan" : s.toString();
        } catch (Exception e) {
            return "Gagal membaca: " + e.getMessage();
        }
    }

    public boolean clear() {
        try { return q("04").contains("44"); } catch (IOException e) { return false; }
    }
}
