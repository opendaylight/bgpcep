/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi;

import java.util.Optional;
import javax.annotation.Nonnull;

/**
 * Holds BGP peer specific constraints of PDU processing.
 *
 */
public interface PeerSpecificParserConstraint {

    /**
     * Looks-up and optionally returns {@link PeerConstraint} service.
     * @param peerConstraintType Class type of the service.
     * @return Optional of the required service, absent if the service is not available.
     */
    @Nonnull <T extends PeerConstraint> Optional<T> getPeerConstraint(@Nonnull Class<T> peerConstraintType);

}
