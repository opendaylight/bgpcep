/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import java.net.InetSocketAddress;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.concepts.KeyMapping;

/**
 * Contains all required dependencies for instantiate a PCEPDispatcher.
 */
@NonNullByDefault
public interface PCEPDispatcherDependencies extends PCEPSessionNegotiatorFactoryDependencies {
    /**
     * Return the address to be bound with the server.
     *
     * @return ip address
     */
    InetSocketAddress getAddress();

    /**
     * RFC2385 key mapping.
     *
     * @return map containing Keys
     */
    KeyMapping getKeys();
}
