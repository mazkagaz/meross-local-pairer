package org.localpairer.merosslib;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

/**
 * Supplies the TLS trust settings used for HTTPS broker API calls.
 *
 * Implementations live in the app module because certificate pinning needs
 * persistent storage. merosslib only applies what the policy returns.
 */
public interface TlsPolicy {
    SSLSocketFactory socketFactoryFor(String url);

    HostnameVerifier hostnameVerifierFor(String url);
}
