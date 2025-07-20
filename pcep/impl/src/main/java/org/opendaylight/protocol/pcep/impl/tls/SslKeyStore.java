/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tls;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev250930.pcep.session.tls.PathType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SslKeyStore {
    private static final Logger LOG = LoggerFactory.getLogger(SslKeyStore.class);

    private SslKeyStore() {
        // Hidden on purpose
    }

    /**
     * InputStream instance of key - key location is on classpath or specific path.
     *
     * @param filename
     *          keystore location
     * @param pathType
     *          keystore location type - "classpath" or "path"
     *
     * @return key as InputStream
     */
    public static InputStream asInputStream(final String filename, final PathType pathType) {
        return switch (pathType) {
            case CLASSPATH -> {
                final var in = SslKeyStore.class.getResourceAsStream(filename);
                if (in != null) {
                    yield in;
                }
                throw new IllegalArgumentException("KeyStore file not found: " + filename);
            }
            case PATH -> {
                LOG.debug("Current dir using System: {}", System.getProperty("user.dir"));
                final var keystorefile = Path.of(filename);
                try {
                    yield Files.newInputStream(keystorefile);
                } catch (IOException e) {
                    throw new IllegalStateException("KeyStore file not found: " + filename, e);
                }
            }
        };
    }
}

