/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.spi;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.bgp.parser.BgpTableTypeImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.BgpTableType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.SendReceive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamilies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.mp.capabilities.add.path.capability.AddressFamiliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv6AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;

public class MultiPathSupportUtilTest {

    private static final BgpTableType AFI_SAFI = new BgpTableTypeImpl(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class);

    @Mock
    private PeerSpecificParserConstraint constraints;

    @Mock
    private MultiPathSupport mpSupport;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testIsTableTypeSupportedPossitive() {
        Mockito.doReturn(Optional.of(this.mpSupport)).when(this.constraints).getPeerConstraint(Mockito.any());
        Mockito.doReturn(true).when(this.mpSupport).isTableTypeSupported(Mockito.any());
        Assert.assertTrue(MultiPathSupportUtil.isTableTypeSupported(this.constraints, AFI_SAFI));
    }

    @Test
    public void testIsTableTypeSupportedNegativeTableTypeNotSupported() {
        Mockito.doReturn(Optional.of(this.mpSupport)).when(this.constraints).getPeerConstraint(Mockito.any());
        Mockito.doReturn(false).when(this.mpSupport).isTableTypeSupported(Mockito.any());
        Assert.assertFalse(MultiPathSupportUtil.isTableTypeSupported(this.constraints, AFI_SAFI));
    }

    @Test
    public void testIsTableTypeSupportedNegativeMpSupportAbsent() {
        Mockito.doReturn(Optional.empty()).when(this.constraints).getPeerConstraint(Mockito.any());
        Assert.assertFalse(MultiPathSupportUtil.isTableTypeSupported(this.constraints, AFI_SAFI));
    }

    @Test
    public void testIsTableTypeSupportedNegativeNull() {
        Assert.assertFalse(MultiPathSupportUtil.isTableTypeSupported(null, AFI_SAFI));
    }

    @Test(expected=NullPointerException.class)
    public void testIsTableTypeSupportedNPE() {
        MultiPathSupportUtil.isTableTypeSupported(null, null);
    }

    @Test
    public void testMapTableTypesFamilies() {
        final List<AddressFamilies> addPathTablesType = new ArrayList<>();
        addPathTablesType.add(new AddressFamiliesBuilder().setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class)
            .setSendReceive(SendReceive.Both).build());
        addPathTablesType.add(new AddressFamiliesBuilder().setAfi(Ipv6AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class)
            .setSendReceive(SendReceive.Send).build());
        final Map<TablesKey, SendReceive> result = MultiPathSupportUtil.mapTableTypesFamilies(addPathTablesType);

        final Map<TablesKey, SendReceive> expected = new HashMap<>();
        expected.put(new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class), SendReceive.Both);
        expected.put(new TablesKey(Ipv6AddressFamily.class, UnicastSubsequentAddressFamily.class), SendReceive.Send);
        assertEquals(expected, result);
    }
}
