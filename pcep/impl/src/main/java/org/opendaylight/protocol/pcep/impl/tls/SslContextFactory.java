/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tls;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.app.config.rev160707.pcep.dispatcher.config.Tls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for setting up TLS connection.
 */
public class SslContextFactory {

    private static final String PROTOCOL = "TLS";
    private final Tls tlsConfig;

    private static final Logger LOG = LoggerFactory
            .getLogger(SslContextFactory.class);

    /**
     * SslContextFactory provides information about the TLS context and configuration.
     * @param tlsConfig
     *            TLS configuration object, contains keystore locations and keystore types
     */
    public SslContextFactory(final Tls tlsConfig) {
        this.tlsConfig = requireNonNull(tlsConfig);
    }

    public SSLContext getServerContext() {
        try {
            final KeyStore ks = KeyStore.getInstance(this.tlsConfig.getKeystoreType().name());
            ks.load(SslKeyStore.asInputStream(this.tlsConfig.getKeystore(), this.tlsConfig.getKeystorePathType()),
                    this.tlsConfig.getKeystorePassword().toCharArray());
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, this.tlsConfig.getCertificatePassword().toCharArray());

            final KeyStore ts = KeyStore.getInstance(this.tlsConfig.getTruststoreType().name());
            ts.load(SslKeyStore.asInputStream(this.tlsConfig.getTruststore(), this.tlsConfig.getTruststorePathType()),
                    this.tlsConfig.getTruststorePassword().toCharArray());
            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);

            final SSLContext serverContext = SSLContext.getInstance(PROTOCOL);
            serverContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return serverContext;
        } catch (final IOException e) {
            LOG.warn(
                "IOException - Failed to load keystore / truststore. Failed to initialize the server-side SSLContext",
                e);
        } catch (final NoSuchAlgorithmException e) {
            LOG.warn(
                "NoSuchAlgorithmException - Unsupported algorithm. Failed to initialize the server-side SSLContext", e);
        } catch (final CertificateException e) {
            LOG.warn("CertificateException - Unable to access certificate (check password). Failed to initialize the server-side SSLContext", e);
        } catch (final Exception e) {
            LOG.warn("Exception - Failed to initialize the server-side SSLContext", e);
        }
        //TODO try to use default SSLContext instance?
        return null;
    }
}

