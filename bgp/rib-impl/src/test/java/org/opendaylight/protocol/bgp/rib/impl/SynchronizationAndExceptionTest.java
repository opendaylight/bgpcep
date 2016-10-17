/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.opendaylight.protocol.bgp.rib.impl.AdjRibInWriter.ATTRIBUTES_UPTODATE_FALSE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;
import org.opendaylight.protocol.bgp.mode.impl.base.BasePathSelectionModeFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.UpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.update.message.NlriBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.RibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

public class SynchronizationAndExceptionTest extends AbstractSynchronization {
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testHandleMessageAfterException() throws InterruptedException {
        final Map<TablesKey, PathSelectionMode> pathTables = ImmutableMap.of(new TablesKey(BGP_TABLE_TYPE.getAfi(), BGP_TABLE_TYPE.getSafi()),
            BasePathSelectionModeFactory.createBestPathSelectionStrategy());
        final RIBImpl ribImpl = new RIBImpl(this.clusterSingletonServiceProvider, new RibId(RIB_ID), AS_NUMBER, new BgpId(RIB_ID), null, READ_ONLY_LIMIT, this.ribExtension,
            this.dispatcher, this.mappingService.getCodecFactory(), this.domBroker, ImmutableList.of(BGP_TABLE_TYPE), pathTables, this.ribExtension
            .getClassLoadingStrategy(), null);
        ribImpl.instantiateServiceInstance();
        ribImpl.onGlobalContextUpdated(this.schemaContext);

        final BGPPeer bgpPeer = new BGPPeer("peer-test", ribImpl, PeerRole.Ibgp, null);
        bgpPeer.instantiateServiceInstance();
        final BGPSessionImpl bgpSession = new BGPSessionImpl(bgpPeer, this.speakerListener, this.classicOpen, this.classicOpen.getHoldTimer(), null);
        bgpSession.setChannelExtMsgCoder(this.classicOpen);
        bgpPeer.onSessionUp(bgpSession);

        final List<Ipv4Prefix> prefs = Lists.newArrayList(new Ipv4Prefix("8.0.1.0/28"), new Ipv4Prefix("127.0.0.1/32"), new Ipv4Prefix("2.2.2.2/24"));
        final UpdateBuilder wrongMessage = new UpdateBuilder();
        wrongMessage.setNlri(new NlriBuilder().setNlri(prefs).build());
        final Origin origin = new OriginBuilder().setValue(BgpOrigin.Igp).build();
        final AsPath asPath = new AsPathBuilder().setSegments(Collections.emptyList()).build();
        final CNextHop nextHop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("127.0.0.1")).build()).build();
        final AttributesBuilder ab = new AttributesBuilder();
        wrongMessage.setAttributes(ab.setOrigin(origin).setAsPath(asPath).setCNextHop(nextHop).build());

        final UpdateBuilder correct = new UpdateBuilder(wrongMessage.build());
        correct.setAttributes(ab.setLocalPref(new LocalPrefBuilder().setPref((long) 100).build()).build());

        bgpSession.handleMessage(correct.build());
        Mockito.verify(this.tx, times(4)).merge(eq(LogicalDatastoreType.OPERATIONAL), any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        bgpSession.handleMessage(wrongMessage.build());
        Mockito.verify(this.tx, times(4)).merge(eq(LogicalDatastoreType.OPERATIONAL), any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        bgpSession.handleMessage(new UpdateBuilder().build());
        Mockito.verify(this.tx, times(4)).merge(eq(LogicalDatastoreType.OPERATIONAL), any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        Mockito.verify(this.tx).delete(eq(LogicalDatastoreType.OPERATIONAL), eq(PEER_PATH));
        Mockito.verify(this.tx, times(0)).merge(eq(LogicalDatastoreType.OPERATIONAL), eq(TABLE_PATH),
            eq(ImmutableNodes.leafNode(ATTRIBUTES_UPTODATE_FALSE.getNodeType(), Boolean.TRUE)));
    }

    @Test
    public void testUseCase1() throws InterruptedException {
        final Map<TablesKey, PathSelectionMode> pathTables = ImmutableMap.of(new TablesKey(BGP_TABLE_TYPE.getAfi(), BGP_TABLE_TYPE.getSafi()),
            BasePathSelectionModeFactory.createBestPathSelectionStrategy());
        final RIBImpl ribImpl = new RIBImpl(this.clusterSingletonServiceProvider, new RibId(RIB_ID), AS_NUMBER, new BgpId(RIB_ID), null, READ_ONLY_LIMIT, this.ribExtension,
            this.dispatcher, this.mappingService.getCodecFactory(), this.domBroker, ImmutableList.of(BGP_TABLE_TYPE), pathTables, this.ribExtension
            .getClassLoadingStrategy(), null);
        ribImpl.instantiateServiceInstance();
        ribImpl.onGlobalContextUpdated(this.schemaContext);

        final BGPPeer bgpPeer = new BGPPeer("peer-test", ribImpl, PeerRole.Ibgp, null);
        bgpPeer.instantiateServiceInstance();
        final BGPSessionImpl bgpSession = new BGPSessionImpl(bgpPeer, this.speakerListener, this.classicOpen, this.classicOpen.getHoldTimer(), null);
        bgpSession.setChannelExtMsgCoder(this.classicOpen);
        bgpPeer.onSessionUp(bgpSession);

        final List<Ipv4Prefix> prefs = Lists.newArrayList(new Ipv4Prefix("8.0.1.0/28"), new Ipv4Prefix("127.0.0.1/32"), new Ipv4Prefix("2.2.2.2/24"));
        final UpdateBuilder wrongMessage = new UpdateBuilder();
        wrongMessage.setNlri(new NlriBuilder().setNlri(prefs).build());
        final Origin origin = new OriginBuilder().setValue(BgpOrigin.Igp).build();
        final AsPath asPath = new AsPathBuilder().setSegments(Collections.emptyList()).build();
        final CNextHop nextHop = new Ipv4NextHopCaseBuilder().setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(new Ipv4Address("127.0.0.1")).build()).build();
        final AttributesBuilder ab = new AttributesBuilder();
        wrongMessage.setAttributes(ab.setOrigin(origin).setAsPath(asPath).setCNextHop(nextHop).build());

        final UpdateBuilder correct = new UpdateBuilder(wrongMessage.build());
        correct.setAttributes(ab.setLocalPref(new LocalPrefBuilder().setPref((long) 100).build()).build());

        bgpSession.handleMessage(correct.build());
        Mockito.verify(this.tx, times(4)).merge(eq(LogicalDatastoreType.OPERATIONAL), any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        bgpSession.handleMessage(new UpdateBuilder().build());
        Mockito.verify(this.tx, times(5)).merge(eq(LogicalDatastoreType.OPERATIONAL), any(YangInstanceIdentifier.class), any(NormalizedNode.class));

        Mockito.verify(this.tx).merge(eq(LogicalDatastoreType.OPERATIONAL), eq(TABLE_PATH),
            eq(ImmutableNodes.leafNode(ATTRIBUTES_UPTODATE_FALSE.getNodeType(), Boolean.TRUE)));
        Mockito.verify(this.tx, times(0)).delete(eq(LogicalDatastoreType.OPERATIONAL), eq(PEER_PATH));
    }
}
