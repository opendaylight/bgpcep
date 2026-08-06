/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.extended.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExtendedCommunityUtilTest {
    @Test
    void testGetType() {
        assertEquals(1, ExtendedCommunityUtil.setTransitivity(1, true));
        assertEquals(65, ExtendedCommunityUtil.setTransitivity(1, false));
    }

    @Test
    void testIsTransitiveType() {
        assertTrue(ExtendedCommunityUtil.isTransitive(2));
        assertFalse(ExtendedCommunityUtil.isTransitive(66));
    }
}
