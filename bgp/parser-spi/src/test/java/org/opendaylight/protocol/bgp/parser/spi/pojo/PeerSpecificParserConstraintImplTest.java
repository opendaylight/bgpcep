/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opendaylight.protocol.bgp.parser.spi.MultiPathSupport;
import org.opendaylight.protocol.bgp.parser.spi.PeerSpecificParserConstraintProvider;

class PeerSpecificParserConstraintImplTest {
    private static final MultiPathSupport ADD_PATH_CONSTRAINT =
        MultiPathSupportImpl.createParserMultiPathSupport(List.of());

    private final PeerSpecificParserConstraintProvider constraints = new PeerSpecificParserConstraintImpl();

    @Test
    void testProviderSucess() {
        assertTrue(constraints.addPeerConstraint(MultiPathSupport.class, ADD_PATH_CONSTRAINT));
    }

    @Test
    void testProviderAlreadyPresent() {
        constraints.addPeerConstraint(MultiPathSupport.class, ADD_PATH_CONSTRAINT);
        assertFalse(constraints.addPeerConstraint(MultiPathSupport.class, ADD_PATH_CONSTRAINT));
    }

    @Test
    void testProviderNullInput() {
        assertThrows(NullPointerException.class, () -> constraints.addPeerConstraint(MultiPathSupport.class, null));
    }

    @Test
    void testGetPeerConstraintSuccess() {
        constraints.addPeerConstraint(MultiPathSupport.class, ADD_PATH_CONSTRAINT);
        final var peerConstraint = constraints.getPeerConstraint(MultiPathSupport.class);
        assertTrue(peerConstraint.isPresent());
    }

    @Test
    void testGetPeerConstraintNonExisting() {
        final var peerConstraint = constraints.getPeerConstraint(MultiPathSupport.class);
        assertTrue(peerConstraint.isEmpty());
    }
}
