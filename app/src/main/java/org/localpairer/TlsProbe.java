package org.localpairer;

import android.annotation.SuppressLint;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Checks that a broker endpoint speaks MQTT over TLS before pairing.
 *
 * The Meross firmware always uses TLS for MQTT, so a plaintext broker would
 * silently fail later. The probe only performs the TLS handshake; it accepts
 * any certificate because it verifies the transport, not the broker identity.
 */
@SuppressLint({"TrustAllX509TrustManager", "CustomX509TrustManager"})
public final class TlsProbe {
    public enum Result {
        OK,
        UNREACHABLE,
        NOT_TLS
    }

    private TlsProbe() {
    }



    /** Returns true when the presented fingerprint matches the pinned one. */
    public static boolean matchesFingerprint(String expected, String presented) {
        return expected != null && presented != null
                && expected.equalsIgnoreCase(presented);
    }

    /** Returns the SHA-256 fingerprint of the TLS leaf certificate, or null. */
    public static String certificateFingerprint(String host, int port) {
        SSLSocket socket = null;
        try {
            SSLContext context = trustAllContext();
            socket = (SSLSocket) context.getSocketFactory().createSocket();
            socket.connect(new InetSocketAddress(host, port), 3000);
            socket.setSoTimeout(3000);
            socket.startHandshake();
            Certificate[] chain = socket.getSession().getPeerCertificates();
            return chain.length == 0 ? null : sha256Hex(chain[0].getEncoded());
        } catch (Exception e) {
            return null;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static SSLContext trustAllContext() throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }}, new SecureRandom());
        return context;
    }

    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte value : hash) {
            String part = Integer.toHexString(0xFF & value);
            if (part.length() == 1) {
                hex.append('0');
            }
            hex.append(part);
        }
        return hex.toString();
    }

    public static Result probe(String host, int port) {
        SSLSocket socket = null;
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
            socket = (SSLSocket) context.getSocketFactory().createSocket();
            try {
                socket.connect(new InetSocketAddress(host, port), 3000);
            } catch (IOException e) {
                return Result.UNREACHABLE;
            }
            try {
                socket.setSoTimeout(3000);
                socket.startHandshake();
                return Result.OK;
            } catch (IOException e) {
                return Result.NOT_TLS;
            }
        } catch (Exception e) {
            return Result.UNREACHABLE;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
