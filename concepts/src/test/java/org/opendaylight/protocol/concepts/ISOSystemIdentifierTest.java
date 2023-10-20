/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.concepts;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.IsoSystemIdentifier;

class ISOSystemIdentifierTest {
    @Test
    void testISOSystemIdentifier() {
        final var ex = assertThrows(IllegalArgumentException.class,
            () -> new IsoSystemIdentifier(new byte[] { 10, 12, 127, 0, 9, 1, 1 }));
        assertEquals("Invalid length: 0a0c7f00090101, expected: [[6..6]].", ex.getMessage());
    }

    @Test
    void testGetBytes() {
        final var id = new IsoSystemIdentifier(new byte[] { 10, 12, 127, 0, 9, 1 });
        assertArrayEquals(new byte[] { 10, 12, 127, 0, 9, 1 }, id.getValue());
    }
}
