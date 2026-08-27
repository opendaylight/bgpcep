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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Optional;
import org.opendaylight.protocol.bgp.parser.spi.PeerConstraint;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraintProvider;

public class PeerSpecificParserConstraintImpl implements PeerSpecificParserConstraintProvider {
    private static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(
                PeerSpecificParserConstraintImpl.class, "constraints", ImmutableClassToInstanceMap.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private volatile ImmutableClassToInstanceMap<PeerConstraint> constraints = ImmutableClassToInstanceMap.of();

    @Override
    public <T extends PeerConstraint> Optional<T> getPeerConstraint(final Class<T> peerConstraintType) {
        return Optional.ofNullable(constraints.getInstance(peerConstraintType));
    }

    @Override
    public <T extends PeerConstraint> boolean addPeerConstraint(final Class<T> classType, final T peerConstraint) {
        requireNonNull(classType);
        requireNonNull(peerConstraint);

        var local = constraints;
        while (!local.containsKey(classType)) {
            final var updated = ImmutableClassToInstanceMap.<PeerConstraint>builder()
                .putAll(local)
                .put(classType, peerConstraint)
                .build();

            final var witness = (ImmutableClassToInstanceMap<PeerConstraint>)
                VH.compareAndExchange(this, local, updated);
            if (witness == local) {
                // Successfully updated, finished
                return true;
            }

            // Raced with another update, retry
            local = witness;
        }
        return false;
    }
}
