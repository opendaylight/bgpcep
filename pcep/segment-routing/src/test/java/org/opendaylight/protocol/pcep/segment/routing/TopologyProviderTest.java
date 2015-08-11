/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.bgpcep.pcep.topology.provider.AbstractPCEPSessionTest;
import org.opendaylight.bgpcep.pcep.topology.provider.AbstractTopologySessionListener;
import org.opendaylight.bgpcep.pcep.topology.provider.Stateful07TopologySessionListenerFactory;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Path1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.SidType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.network.topology.topology.node.path.computation.client.reported.lsp.path.ero.subobject.subobject.type.SrEroType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.pcrpt.pcrpt.message.reports.path.ero.subobject.subobject.type.SrEroTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.sr.subobject.nai.IpNodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev150112.sr.subobject.nai.IpNodeIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.setup.type.tlv.PathSetupTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.ReportedLsp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;

public class TopologyProviderTest extends AbstractPCEPSessionTest<Stateful07TopologySessionListenerFactory> {

    private AbstractTopologySessionListener<SrpIdNumber, PlspId> listener;
    private PCEPSession session;

    @Override
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.listener = (AbstractTopologySessionListener<SrpIdNumber, PlspId>) getSessionListener();
        this.session = getPCEPSession(getLocalPref(), getRemotePref());
    }

    @Test
    public void testOnReportMessage() throws InterruptedException, ExecutionException {
        this.listener.onSessionUp(this.session);

        Pcrpt pcRptMsg = createSrPcRpt("1.1.1.1", "sr-path1", 1L, true);
        this.listener.onMessage(this.session, pcRptMsg);
        //check sr-path
        Topology topology = getTopology().get();
        List<ReportedLsp> reportedLsps = topology.getNode().get(0).getAugmentation(Node1.class).getPathComputationClient().getReportedLsp();
        Assert.assertEquals(1, reportedLsps.size());
        final ReportedLsp lsp = reportedLsps.get(0);
        Assert.assertEquals("sr-path1", lsp.getName());
        Assert.assertEquals(1, lsp.getPath().get(0).getAugmentation(Path1.class).getPathSetupType().getPst().intValue());
        List<Subobject> subobjects = lsp.getPath().get(0).getEro().getSubobject();
        Assert.assertEquals(1, subobjects.size());
        Assert.assertEquals("1.1.1.1", ((IpNodeId)((SrEroType)subobjects.get(0).getSubobjectType()).getNai()).getIpAddress().getIpv4Address().getValue());

        pcRptMsg = createSrPcRpt("1.1.1.3", "sr-path2", 2L, false);
        this.listener.onMessage(this.session, pcRptMsg);
        //check second lsp sr-path
        topology = getTopology().get();
        reportedLsps = topology.getNode().get(0).getAugmentation(Node1.class).getPathComputationClient().getReportedLsp();
        Assert.assertEquals(2, reportedLsps.size());

        pcRptMsg = createSrPcRpt("1.1.1.2", "sr-path1", 1L, true);
        this.listener.onMessage(this.session, pcRptMsg);
        //check updated sr-path
        topology = getTopology().get();
        reportedLsps = topology.getNode().get(0).getAugmentation(Node1.class).getPathComputationClient().getReportedLsp();
        Assert.assertEquals(2, reportedLsps.size());
        for (final ReportedLsp rlsp : reportedLsps) {
            if (rlsp.getName().equals("sr-path1")) {
                subobjects = rlsp.getPath().get(0).getEro().getSubobject();
                Assert.assertEquals(1, subobjects.size());
                Assert.assertEquals("1.1.1.2", ((IpNodeId)((SrEroType)subobjects.get(0).getSubobjectType()).getNai()).getIpAddress().getIpv4Address().getValue());
            }
        }
    }

    private static Pcrpt createSrPcRpt(final String nai, final String pathName, final long plspId, final boolean hasLspIdTlv) {
        final TlvsBuilder lspTlvBuilder = new TlvsBuilder();
        if (hasLspIdTlv) {
            lspTlvBuilder.setLspIdentifiers(new LspIdentifiersBuilder().setLspId(new LspId(plspId)).build());
        }
        return new PcrptBuilder().setPcrptMessage(new PcrptMessageBuilder().setReports(Lists.newArrayList(new ReportsBuilder()
            .setLsp(new LspBuilder().setPlspId(new PlspId(plspId)).setRemove(false).setSync(true).setAdministrative(true).setDelegate(true)
                    .setTlvs(lspTlvBuilder
                        .setSymbolicPathName(new SymbolicPathNameBuilder().setPathName(new SymbolicPathName(pathName.getBytes(Charsets.UTF_8))).build()).build()).build())
            .setSrp(new SrpBuilder().setOperationId(new SrpIdNumber(0L)).setTlvs(
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.srp.TlvsBuilder()
                        .setPathSetupType(new PathSetupTypeBuilder().setPst((short) 1).build()).build()).build())
            .setPath(new PathBuilder().setEro(createSrEroObject(nai)).build())
            .build())).build()).build();
    }

    private static Ero createSrEroObject(final String nai) {
        final SrEroTypeBuilder srEroBuilder = new SrEroTypeBuilder();
        srEroBuilder.setCFlag(false);
        srEroBuilder.setMFlag(false);
        srEroBuilder.setSidType(SidType.Ipv4NodeId);
        srEroBuilder.setSid(123456L);
        srEroBuilder.setNai(new IpNodeIdBuilder().setIpAddress(new IpAddress(new Ipv4Address(nai))).build());
        final SubobjectBuilder subobjBuilder = new SubobjectBuilder().setSubobjectType(srEroBuilder.build()).setLoose(false);

        final List<Subobject> subobjects = Lists.newArrayList(subobjBuilder.build());
        return new EroBuilder().setProcessingRule(false).setIgnore(false).setSubobject(subobjects).build();
    }

}
