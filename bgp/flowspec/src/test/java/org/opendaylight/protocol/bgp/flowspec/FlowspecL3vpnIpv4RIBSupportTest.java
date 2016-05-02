/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.protocol.bgp.flowspec.l3vpn.ipv4.FlowspecL3vpnIpv4RIBSupport;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.FlowspecL3vpnSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.Flowspec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.ipv4.route.FlowspecRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpReachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.multiprotocol.rev130919.update.attributes.MpUnreachNlri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.RouteDistinguisherBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;

/**
 * @author Kevin Wang
 */
public class FlowspecL3vpnIpv4RIBSupportTest {
    private static final NodeIdentifier RD_NID = new NodeIdentifier(QName.create(Flowspec.QNAME.getNamespace(), Flowspec.QNAME.getRevision(), "route-distinguisher"));
    private static final Ipv4Address ipv4 = new Ipv4Address("42.42.42.42");
    private static final String ROUTE_DISTINGUISHER = "1.2.3.4:10";

    private final AbstractFlowspecRIBSupport ribSupport = FlowspecL3vpnIpv4RIBSupport.getInstance(new FlowspecExtensionProviderContext());
    private final List<MapEntryNode> fsList = new ArrayList<>();
    private List<YangInstanceIdentifier> routes;
    @Mock
    private DOMDataWriteTransaction tx;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        routes = new ArrayList<>();
        Mockito.doAnswer(
            (final InvocationOnMock invocation) -> {
                final Object[] args = invocation.getArguments();
                FlowspecL3vpnIpv4RIBSupportTest.this.routes.add((YangInstanceIdentifier) args[1]);
                return args[1];
            }
        ).when(this.tx).put(Mockito.any(LogicalDatastoreType.class), Mockito.any(YangInstanceIdentifier.class), Mockito.any(NormalizedNode.class));

        Mockito.doAnswer(
            (final InvocationOnMock invocation) -> {
                final Object[] args = invocation.getArguments();
                FlowspecL3vpnIpv4RIBSupportTest.this.routes.remove(args[1]);
                return args[1];
            }
        ).when(this.tx).delete(Mockito.any(LogicalDatastoreType.class), Mockito.any(YangInstanceIdentifier.class));

        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> entry = Builders.mapEntryBuilder();
        entry.withNodeIdentifier(new NodeIdentifierWithPredicates(FlowspecRoute.QNAME, FlowspecRoute.QNAME, entry));
        entry.withChild(
            Builders.leafBuilder()
                .withNodeIdentifier(RD_NID)
                .withValue(
                    RouteDistinguisherBuilder.getDefaultInstance(ROUTE_DISTINGUISHER)
                ).build()
        );

        fsList.add(entry.build());
    }

    @Test
    public void testbuildReach() throws BGPParsingException {
        final CNextHop hop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(ipv4).build()).build();
        final MpReachNlri result = ribSupport.buildReach(fsList, hop);
        assertEquals(Ipv4AddressFamily.class, result.getAfi());
        assertEquals(FlowspecL3vpnSubsequentAddressFamily.class, result.getSafi());
        assertEquals(new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("42.42.42.42")).build()).build(), result.getCNextHop());
    }

    @Test
    public void testBuildUnreach() {
        final MpUnreachNlri result = ribSupport.buildUnreach(fsList);
        assertEquals(Ipv4AddressFamily.class, result.getAfi());
        assertEquals(FlowspecL3vpnSubsequentAddressFamily.class, result.getSafi());
    }
}