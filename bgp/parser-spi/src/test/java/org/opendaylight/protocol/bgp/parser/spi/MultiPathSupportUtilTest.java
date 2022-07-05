/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev180329.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class MultiPathSupportUtilTest {
    private static final BgpTableType AFI_SAFI =
        new BgpTableTypeImpl(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE);

    @Mock
    private PeerSpecificParserConstraint constraints;

    @Mock
    private MultiPathSupport mpSupport;

    @Test
    public void testIsTableTypeSupportedPossitive() {
        doReturn(Optional.of(this.mpSupport)).when(this.constraints).getPeerConstraint(any());
        doReturn(true).when(this.mpSupport).isTableTypeSupported(any());
        assertTrue(MultiPathSupportUtil.isTableTypeSupported(this.constraints, AFI_SAFI));
    }

    @Test
    public void testIsTableTypeSupportedNegativeTableTypeNotSupported() {
        doReturn(Optional.of(this.mpSupport)).when(this.constraints).getPeerConstraint(any());
        doReturn(false).when(this.mpSupport).isTableTypeSupported(any());
        assertFalse(MultiPathSupportUtil.isTableTypeSupported(this.constraints, AFI_SAFI));
    }

    @Test
    public void testIsTableTypeSupportedNegativeMpSupportAbsent() {
        doReturn(Optional.empty()).when(this.constraints).getPeerConstraint(any());
        assertFalse(MultiPathSupportUtil.isTableTypeSupported(this.constraints, AFI_SAFI));
    }

    @Test
    public void testIsTableTypeSupportedNegativeNull() {
        assertFalse(MultiPathSupportUtil.isTableTypeSupported(null, AFI_SAFI));
    }

    @Test
    public void testIsTableTypeSupportedNPE() {
        assertThrows(NullPointerException.class, () -> MultiPathSupportUtil.isTableTypeSupported(null, null));
    }
}
