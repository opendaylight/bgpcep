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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.app.config.rev160707.PathType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.app.config.rev160707.StoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.app.config.rev160707.pcep.dispatcher.config.Tls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.app.config.rev160707.pcep.dispatcher.config.TlsBuilder;

public class SslContextFactoryTest {

    @Test
    public void testSslContextFactory() {
        final SslContextFactory sslContextFactory = new SslContextFactory(createTlsConfig());
        final SSLContext sslContext = sslContextFactory.getServerContext();
        assertNotNull(sslContext);
    }

    public static Tls createTlsConfig() {
        return new TlsBuilder().setCertificatePassword("opendaylight").setKeystore("/exemplary-ctlKeystore")
                .setKeystorePassword("opendaylight").setKeystorePathType(PathType.CLASSPATH)
                .setKeystoreType(StoreType.JKS).setTruststore("/exemplary-ctlTrustStore")
                .setTruststorePassword("opendaylight").setTruststorePathType(PathType.CLASSPATH)
                .setTruststoreType(StoreType.JKS).build();
    }
}
