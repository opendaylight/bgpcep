/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;

@ExtendWith(MockitoExtension.class)
class MultiPathSupportUtilTest {
    private static final BgpTableType AFI_SAFI =
        new BgpTableTypeImpl(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE);

    @Mock
    private PeerSpecificParserConstraint constraints;

    @Mock
    private MultiPathSupport mpSupport;

    @Test
    void testIsTableTypeSupportedPossitive() {
        doReturn(Optional.of(this.mpSupport)).when(this.constraints).getPeerConstraint(any());
        doReturn(true).when(this.mpSupport).isTableTypeSupported(any());
        assertTrue(MultiPathSupportUtil.isTableTypeSupported(this.constraints, AFI_SAFI));
    }

    @Test
    void testIsTableTypeSupportedNegativeTableTypeNotSupported() {
        doReturn(Optional.of(this.mpSupport)).when(this.constraints).getPeerConstraint(any());
        doReturn(false).when(this.mpSupport).isTableTypeSupported(any());
        assertFalse(MultiPathSupportUtil.isTableTypeSupported(this.constraints, AFI_SAFI));
    }

    @Test
    void testIsTableTypeSupportedNegativeMpSupportAbsent() {
        doReturn(Optional.empty()).when(this.constraints).getPeerConstraint(any());
        assertFalse(MultiPathSupportUtil.isTableTypeSupported(this.constraints, AFI_SAFI));
    }

    @Test
    void testIsTableTypeSupportedNegativeNull() {
        assertFalse(MultiPathSupportUtil.isTableTypeSupported(null, AFI_SAFI));
    }

    @Test
    void testIsTableTypeSupportedNPE() {
        assertThrows(NullPointerException.class, () -> MultiPathSupportUtil.isTableTypeSupported(null, null));
    }
}
