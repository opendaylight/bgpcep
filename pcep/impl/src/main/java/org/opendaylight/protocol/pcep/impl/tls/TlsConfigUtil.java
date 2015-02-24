/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tls;

public final class TlsConfigUtil {

    public static final TlsConfiguration createDummyTlsConfiguration() {
        return new TlsConfigurationImpl(KeystoreType.JKS,
                "/selfSignedXRVR", PathType.CLASSPATH, KeystoreType.JKS,
                "/selfSignedODL", PathType.CLASSPATH) ;
    }
}
