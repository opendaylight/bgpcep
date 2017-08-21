/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi.pojo;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.protocol.bgp.parser.spi.PeerConstraint;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraintProvider;

public class PeerSpecificParserConstraintImpl implements PeerSpecificParserConstraintProvider {

    @GuardedBy("this")
    private final Map<Class<? extends PeerConstraint>, PeerConstraint> constraints = new HashMap<>();

    @Override
    public synchronized <T extends PeerConstraint> Optional<T> getPeerConstraint(final Class<T> peerConstraintType) {
        return (Optional<T>) Optional.ofNullable(this.constraints.get(peerConstraintType));
    }

    @Override
    public synchronized <T extends PeerConstraint> boolean addPeerConstraint(final Class<T> classType, final T peerConstraint) {
        requireNonNull(classType);
        requireNonNull(peerConstraint);
        final PeerConstraint previous = this.constraints.putIfAbsent(classType, peerConstraint);
        return previous == null;
    }

}
