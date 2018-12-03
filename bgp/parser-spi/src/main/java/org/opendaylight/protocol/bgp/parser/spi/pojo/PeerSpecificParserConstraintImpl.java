/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi.pojo;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap.Builder;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.opendaylight.protocol.bgp.parser.spi.PeerConstraint;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraintProvider;

public class PeerSpecificParserConstraintImpl implements PeerSpecificParserConstraintProvider {
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<PeerSpecificParserConstraintImpl, ImmutableClassToInstanceMap>
            CONSTRAINTS_UPDATER = AtomicReferenceFieldUpdater.newUpdater(PeerSpecificParserConstraintImpl.class,
                ImmutableClassToInstanceMap.class, "constraints");

    private volatile ImmutableClassToInstanceMap<PeerConstraint> constraints = ImmutableClassToInstanceMap.of();

    @Override
    public <T extends PeerConstraint> Optional<T> getPeerConstraint(final Class<T> peerConstraintType) {
        return Optional.ofNullable(constraints.getInstance(peerConstraintType));
    }

    @Override
    public <T extends PeerConstraint> boolean addPeerConstraint(final Class<T> classType, final T peerConstraint) {
        requireNonNull(classType);
        requireNonNull(peerConstraint);

        ImmutableClassToInstanceMap<PeerConstraint> local = constraints;
        while (!local.containsKey(classType)) {
            final Builder<PeerConstraint> builder = ImmutableClassToInstanceMap.builder();
            builder.putAll(local);
            builder.put(classType, peerConstraint);
            if (CONSTRAINTS_UPDATER.compareAndSet(this, local, builder.build())) {
                // Successfully updated, finished
                return true;
            }

            // Raced with another update, retry
            local = constraints;
        }
        return false;
    }
}
