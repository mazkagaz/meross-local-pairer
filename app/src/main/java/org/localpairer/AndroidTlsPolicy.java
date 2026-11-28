package org.localpairer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import org.localpairer.merosslib.MerossHttpClient;
import org.localpairer.merosslib.TlsPolicy;

import java.net.URI;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Trust-on-first-use pinning for the broker HTTPS endpoint.
 *
 * The local broker uses a self-signed certificate, so the system trust store
 * cannot validate it. Instead the leaf certificate digest is stored on first
 * use and every later connection must present the same certificate. Hostname
 * verification is replaced by that pin, which also allows reaching the broker
 * through an IP address.
 */
@SuppressLint({"TrustAllX509TrustManager", "CustomX509TrustManager"})
public final class AndroidTlsPolicy implements TlsPolicy {
    private static final String TAG = "LocalPairerTls";
    static final String PIN_MISMATCH = "MEROSS_PIN_MISMATCH";
    private static final Map<String, String> MISMATCH_DESCRIPTIONS =
            new ConcurrentHashMap<>();

    private final Context context;

    public AndroidTlsPolicy(Context context) {
        this.context = context.getApplicationContext();
    }

    public static void install(Context context) {
        MerossHttpClient.setTlsPolicy(new AndroidTlsPolicy(context));
    }

    /** Returns the fingerprint stored for the endpoint, or null. */
    public static String pinnedFingerprint(Context context, String url) {
        String key = hostKey(url);
        return key == null ? null : TlsPinStore.get(context, key);
    }


    /** Pin a certificate fingerprint after an explicit user confirmation. */
    public static void pinFingerprint(Context context, String url, String fingerprint) {
        String key = hostKey(url);
        if (key != null && fingerprint != null && !fingerprint.isEmpty()) {
            TlsPinStore.store(context, key, fingerprint);
        }
    }

    public static void clearPin(Context context, String url) {
        String key = hostKey(url);
        if (key != null) {
            TlsPinStore.clear(context, key);
        }
    }

    /** Returns a user-facing message when the exception is a pin mismatch. */
    public static String pinMismatchMessage(Throwable error) {
        Throwable current = error;
        int depth = 0;
        while (current != null && depth < 10) {
            String message = current.getMessage();
            if (message != null && message.contains(PIN_MISMATCH)) {
                return "The broker TLS certificate changed.";
            }
            current = current.getCause();
            depth++;
        }
        return null;
    }

    /**
     * Description of the certificate presented by the broker during the last
     * mismatch, or a generic text when it is not available anymore.
     */
    public static String mismatchDescription(String url) {
        String key = hostKey(url);
        String description = key == null ? null : MISMATCH_DESCRIPTIONS.get(key);
        return description != null
                ? description
                : "The new certificate details are no longer available.";
    }

    @Override
    public SSLSocketFactory socketFactoryFor(String url) {
        String key = hostKey(url);
        if (key == null) {
            return null;
        }
        try {
            final String hostKey = key;
            final X509TrustManager trustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                    if (chain == null || chain.length == 0) {
                        throw new CertificateException("No server certificate received");
                    }
                    String presented = fingerprint(chain[0]);
                    String pinned = TlsPinStore.get(context, hostKey);
                    if (pinned == null) {
                        TlsPinStore.store(context, hostKey, presented);
                        PairingLog.log("tls", "pinned certificate for " + hostKey
                                + " sha256=" + presented);
                        return;
                    }
                    if (!pinned.equalsIgnoreCase(presented)) {
                        PairingLog.log("tls", "certificate mismatch for " + hostKey
                                + " expected=" + pinned + " presented=" + presented);
                        MISMATCH_DESCRIPTIONS.put(hostKey, describe(chain[0]));
                        throw new CertificateException(PIN_MISMATCH + " for " + hostKey
                                + ": expected sha256 " + pinned + ", got " + presented);
                    }
                    MISMATCH_DESCRIPTIONS.remove(hostKey);
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            Log.e(TAG, "Could not build the TLS socket factory", e);
            return null;
        }
    }

    @Override
    public HostnameVerifier hostnameVerifierFor(String url) {
        // The certificate pin identifies the broker; a local endpoint may be
        // reached by hostname, mDNS name or IP address.
        return (hostname, session) -> hostKey(url) != null;
    }

    private static String hostKey(String url) {
        if (url == null) {
            return null;
        }
        try {
            URI uri = new URI(url.trim());
            String host = uri.getHost();
            if (host == null || host.isEmpty()) {
                return null;
            }
            int port = uri.getPort();
            if (port <= 0) {
                port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
            }
            return host.toLowerCase(Locale.ROOT) + ":" + port;
        } catch (Exception e) {
            return null;
        }
    }

    private static String describe(X509Certificate certificate) {
        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return "Subject: " + certificate.getSubjectX500Principal().getName()
                + "\nIssuer: " + certificate.getIssuerX500Principal().getName()
                + "\nValid until: " + date.format(certificate.getNotAfter())
                + "\nSHA-256: " + fingerprint(certificate);
    }

    private static String fingerprint(X509Certificate certificate) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(certificate.getEncoded());
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                String part = Integer.toHexString(0xFF & value);
                if (part.length() == 1) {
                    hex.append('0');
                }
                hex.append(part);
            }
            return hex.toString();
        } catch (Exception e) {
            return certificate.getSerialNumber().toString(16);
        }
    }
}
