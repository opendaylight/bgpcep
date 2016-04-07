/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi;

import javax.annotation.Nonnull;

/**
 * Serve as an entry gate for providers of {@link PeerSpecificParserConstraint} services.
 *
 */
public interface PeerSpecificParserConstraintProvider extends PeerSpecificParserConstraint {

    /**
     * Register new {@link PeerConstraint} service.
     * @param classType Class type of the service to be added.
     * @param peerConstraint Peer constraint service to be added.
     * @return True if service was added, false if not (such service is already registered).
     */
    <T extends PeerConstraint> boolean addPeerConstraint(@Nonnull Class<T> classType, @Nonnull T peerConstraint);

}
