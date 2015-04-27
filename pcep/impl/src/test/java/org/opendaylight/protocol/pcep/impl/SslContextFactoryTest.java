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
import org.opendaylight.controller.config.yang.pcep.impl.Tls;
import org.opendaylight.protocol.pcep.impl.tls.SslContextFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.impl.rev130627.PathType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.impl.rev130627.StoreType;

public class SslContextFactoryTest {

    @Test
    public void testSslContextFactory() {
        final SslContextFactory sslContextFactory = new SslContextFactory(createTlsConfig());
        final SSLContext sslContext = sslContextFactory.getServerContext();
        assertNotNull(sslContext);
    }

    public static Tls createTlsConfig() {
        final Tls tlsConfig = new Tls();
        tlsConfig.setCertificatePassword("opendaylight");
        tlsConfig.setKeystore("/exemplary-ctlKeystore");
        tlsConfig.setKeystorePassword("opendaylight");
        tlsConfig.setKeystorePathType(PathType.CLASSPATH);
        tlsConfig.setKeystoreType(StoreType.JKS);
        tlsConfig.setTruststore("/exemplary-ctlTrustStore");
        tlsConfig.setTruststorePassword("opendaylight");
        tlsConfig.setTruststorePathType(PathType.CLASSPATH);
        tlsConfig.setTruststoreType(StoreType.JKS);
        return tlsConfig;
    }

}
