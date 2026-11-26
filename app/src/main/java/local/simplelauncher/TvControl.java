package local.simplelauncher;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.HttpURLConnection;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sends a remote-control code to this TV over the network, using Sony's IRCC-IP. This is how a button can
 * do things an ordinary app can't do on its own, such as Channel up/down. It needs these switched on in the
 * TV's Settings: Network and Internet, Home network, IP control, with Authentication set to
 * "Normal and Pre-Shared Key" and a Pre-Shared Key entered; the same key is entered in the Button mapper.
 */
final class TvControl {
    private TvControl() {}

    static final String PSK = "tv_psk";

    private static final ExecutorService NET = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    static boolean hasKey(Context c) {
        return !Prefs.getString(c, PSK, "").isEmpty();
    }

    /** Send the named Sony code (see {@link ActionCatalog#SONY_CODES}); problems are shown as a message. */
    static void send(Context ctx, String name) {
        final Context app = ctx.getApplicationContext();
        final String code = ActionCatalog.sonyCode(name);
        final String psk = Prefs.getString(app, PSK, "");
        if (code == null) {
            toast(app, "Unknown TV function " + name);
            return;
        }
        if (psk.isEmpty()) {
            toast(app, "Set the TV's IP control key first (Launcher settings > Button mapper)");
            return;
        }
        NET.execute(() -> {
            String ip = localAddress(app);
            if (ip == null) {
                toast(app, "No network connection to the TV");
                return;
            }
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL("http://" + ip + "/sony/ircc").openConnection();
                c.setConnectTimeout(2000);
                c.setReadTimeout(3000);
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
                c.setRequestProperty("SOAPACTION", "\"urn:schemas-sony-com:service:IRCC:1#X_SendIRCC\"");
                c.setRequestProperty("X-Auth-PSK", psk);
                byte[] body = envelope(code).getBytes(StandardCharsets.UTF_8);
                c.setFixedLengthStreamingMode(body.length);
                try (OutputStream out = c.getOutputStream()) {
                    out.write(body);
                }
                int status = c.getResponseCode();
                if (status == 403 || status == 401) {
                    toast(app, "The TV refused the key: check the IP control key matches the TV's");
                } else if (status < 200 || status >= 300) {
                    toast(app, "The TV answered with error " + status);
                }
            } catch (Exception e) {
                toast(app, "Couldn't reach the TV's IP control (is it switched on in the TV's settings?)");
            } finally {
                if (c != null) c.disconnect();
            }
        });
    }

    static String envelope(String code) {
        return "<?xml version=\"1.0\"?>"
                + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">"
                + "<s:Body><u:X_SendIRCC xmlns:u=\"urn:schemas-sony-com:service:IRCC:1\">"
                + "<IRCCCode>" + code + "</IRCCCode></u:X_SendIRCC></s:Body></s:Envelope>";
    }

    /** This TV's own address on the network (the TV's IP control listens there, not on 127.0.0.1). */
    private static String localAddress(Context app) {
        try {
            ConnectivityManager cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network n = cm.getActiveNetwork();
            LinkProperties lp = n == null ? null : cm.getLinkProperties(n);
            if (lp != null) {
                for (LinkAddress la : lp.getLinkAddresses()) {
                    InetAddress a = la.getAddress();
                    if (a instanceof Inet4Address && !a.isLoopbackAddress()) return a.getHostAddress();
                }
            }
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && !a.isLoopbackAddress()) return a.getHostAddress();
                }
            }
        } catch (Exception e) {
            // fall through
        }
        return null;
    }

    private static void toast(Context app, String text) {
        MAIN.post(() -> Toast.makeText(app, text, Toast.LENGTH_LONG).show());
    }
}
