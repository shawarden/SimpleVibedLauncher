package local.simplelauncher;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.RouteInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/** A short text summary of the current network connection. */
final class NetInfo {
    private NetInfo() {}

    static String describe(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network net = cm == null ? null : cm.getActiveNetwork();
        NetworkCapabilities caps = net == null ? null : cm.getNetworkCapabilities(net);
        if (caps == null) return "Not connected to a network.";

        StringBuilder sb = new StringBuilder();
        boolean wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        boolean ethernet = caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
        String type = wifi ? "Wi-Fi" : ethernet ? "Ethernet" : "Other";

        if (wifi) {
            WifiInfo wi = wifiInfo(ctx);
            String ssid = wi == null ? null : cleanSsid(wi.getSSID());
            line(sb, "Network", ssid != null ? ssid : "(name hidden by Android: allow Location for this app)");
            line(sb, "Connection", type);
            if (wi != null) {
                int rssi = wi.getRssi();
                if (rssi > -127) line(sb, "Signal", quality(rssi) + " (" + rssi + " dBm)");
                int mhz = wi.getFrequency();
                if (mhz > 0) line(sb, "Band", band(mhz));
                int speed = wi.getLinkSpeed();
                if (speed > 0) line(sb, "Link speed", speed + " Mbps");
            }
        } else {
            line(sb, "Connection", type);
        }

        LinkProperties lp = cm.getLinkProperties(net);
        if (lp != null) {
            for (android.net.LinkAddress la : lp.getLinkAddresses()) {
                if (la.getAddress() instanceof Inet4Address) {
                    line(sb, "IP address", la.getAddress().getHostAddress() + "/" + la.getPrefixLength());
                    break;
                }
            }
            for (RouteInfo r : lp.getRoutes()) {
                if (r.isDefaultRoute() && r.getGateway() instanceof Inet4Address) {
                    line(sb, "Gateway", r.getGateway().getHostAddress());
                    break;
                }
            }
            List<String> dns = new ArrayList<>();
            for (InetAddress d : lp.getDnsServers()) {
                if (d instanceof Inet4Address) dns.add(d.getHostAddress());
            }
            if (!dns.isEmpty()) line(sb, "DNS", join(dns));
            if (lp.getInterfaceName() != null) line(sb, "Interface", lp.getInterfaceName());
        }
        line(sb, "Internet", caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                ? "connected" : "not confirmed");
        return sb.toString().trim();
    }

    @SuppressWarnings("deprecation")
    private static WifiInfo wifiInfo(Context ctx) {
        try {
            WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            return wm == null ? null : wm.getConnectionInfo();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Wi-Fi names come back in quotes, or as "<unknown ssid>" when Android is hiding them. */
    private static String cleanSsid(String raw) {
        if (raw == null || raw.isEmpty() || raw.contains("<unknown ssid>")) return null;
        if (raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }

    private static String quality(int rssi) {
        if (rssi >= -50) return "Excellent";
        if (rssi >= -60) return "Good";
        if (rssi >= -70) return "Fair";
        return "Weak";
    }

    private static String band(int mhz) {
        if (mhz >= 5925) return "6 GHz";
        if (mhz >= 4900) return "5 GHz";
        return "2.4 GHz";
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(p);
        }
        return sb.toString();
    }

    private static void line(StringBuilder sb, String name, String value) {
        sb.append(name).append(": ").append(value).append('\n');
    }
}
