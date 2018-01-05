/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.protocol.bgp.parser.BGPError;

public class TerminationReasonTest {

    @Test
    public void testTerminationReason() {
        assertEquals(BGPError.BAD_PEER_AS.toString(), new BGPTerminationReason(BGPError.BAD_PEER_AS)
                .getErrorMessage());
        assertEquals("BGPTerminationReason{error=BAD_PEER_AS}",
                new BGPTerminationReason(BGPError.BAD_PEER_AS).toString());
    }

}
