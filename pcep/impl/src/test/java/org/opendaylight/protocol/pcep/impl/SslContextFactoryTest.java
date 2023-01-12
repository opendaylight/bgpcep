/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.junit.Assert.assertNotNull;

import javax.net.ssl.SSLContext;
import org.junit.Test;
import org.opendaylight.protocol.pcep.impl.tls.SslContextFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev230112.PcepSessionTls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev230112.pcep.config.session.config.TlsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev230112.pcep.session.tls.PathType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev230112.pcep.session.tls.StoreType;

public class SslContextFactoryTest {

    @Test
    public void testSslContextFactory() {
        final SslContextFactory sslContextFactory = new SslContextFactory(createTlsConfig());
        final SSLContext sslContext = sslContextFactory.getServerContext();
        assertNotNull(sslContext);
    }

    public static PcepSessionTls createTlsConfig() {
        return new TlsBuilder()
            .setCertificatePassword("opendaylight")
            .setKeystore("/exemplary-ctlKeystore")
            .setKeystorePassword("opendaylight")
            .setKeystorePathType(PathType.CLASSPATH)
            .setKeystoreType(StoreType.JKS)
            .setTruststore("/exemplary-ctlTrustStore")
            .setTruststorePassword("opendaylight")
            .setTruststorePathType(PathType.CLASSPATH)
            .setTruststoreType(StoreType.JKS)
            .build();
    }
}
