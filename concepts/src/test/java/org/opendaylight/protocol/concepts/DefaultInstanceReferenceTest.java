/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

class DefaultInstanceReferenceTest {
    private static final InstanceIdentifier<NetworkTopology> IID = InstanceIdentifier.create(NetworkTopology.class);

    @Test
    void testDefaultInstanceReference() {
        final var defaultIID = new DefaultInstanceReference<>(IID);
        assertEquals(IID, defaultIID.getInstanceIdentifier());
    }

    @Test
    void testNullReference() {
        assertThrows(NullPointerException.class, () -> new DefaultInstanceReference<>(null));
    }
}
