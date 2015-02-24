/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.tls;

public interface TlsConfiguration {

    /**
     * @return keystore location
     */
    String getTlsKeystore();

    /**
     * @return keystore type
     */
    KeystoreType getTlsKeystoreType();

    /**
     * @return truststore location
     */
    String getTlsTruststore();

    /**
     * @return truststore type
     */
    KeystoreType getTlsTruststoreType();

    /**
     * @return keystore path type (CLASSPATH or PATH)
     */
    PathType getTlsKeystorePathType();

    /**
     * @return truststore path type (CLASSPATH or PATH)
     */
    PathType getTlsTruststorePathType();

    /**
     * @return password protecting specified keystore
     */
    String getKeystorePassword();

    /**
     * @return password protecting certificate
     */
    String getCertificatePassword();

    /**
     * @return password protecting specified truststore
     */
    String getTruststorePassword();
}

