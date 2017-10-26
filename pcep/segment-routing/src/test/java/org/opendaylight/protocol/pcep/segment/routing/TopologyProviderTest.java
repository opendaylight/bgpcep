/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.opendaylight.protocol.util.CheckUtil.readDataOperational;

import com.google.common.collect.Lists;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.bgpcep.pcep.topology.provider.AbstractPCEPSessionTest;
import org.opendaylight.bgpcep.pcep.topology.provider.AbstractTopologySessionListener;
import org.opendaylight.bgpcep.pcep.topology.provider.Stateful07TopologySessionListenerFactory;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Path1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.SidType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.network.topology.topology.node.path.computation.client.reported.lsp.path.ero.subobject.subobject.type.SrEroType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.pcrpt.pcrpt.message.reports.path.ero.subobject.subobject.type.SrEroTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.sr.subobject.nai.IpNodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.segment.routing.rev171025.sr.subobject.nai.IpNodeIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.setup.type.tlv.PathSetupTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.ReportedLsp;

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
    public void testOnReportMessage() throws ReadFailedException {
        this.listener.onSessionUp(this.session);

        Pcrpt pcRptMsg = createSrPcRpt("1.1.1.1", "sr-path1", 1L, true);
        this.listener.onMessage(this.session, pcRptMsg);
        readDataOperational(getDataBroker(), this.pathComputationClientIId, pcc -> {
            //check sr-path
            final List<ReportedLsp> reportedLsps = pcc.getReportedLsp();
            assertNotNull(reportedLsps);
            assertEquals(1, reportedLsps.size());
            final ReportedLsp lsp = reportedLsps.get(0);
            assertEquals("sr-path1", lsp.getName());
            assertEquals(1, lsp.getPath().get(0).getAugmentation(Path1.class).getPathSetupType()
                .getPst().intValue());
            final List<Subobject> subobjects = lsp.getPath().get(0).getEro().getSubobject();
            assertEquals(1, subobjects.size());
            assertEquals("1.1.1.1", ((IpNodeId)((SrEroType)subobjects.get(0).getSubobjectType())
                .getNai()).getIpAddress().getIpv4Address().getValue());
            return pcc;
        });

        pcRptMsg = createSrPcRpt("1.1.1.3", "sr-path2", 2L, false);
        this.listener.onMessage(this.session, pcRptMsg);
        readDataOperational(getDataBroker(), this.pathComputationClientIId, pcc -> {
            //check second lsp sr-path
            final List<ReportedLsp> reportedLsps = pcc.getReportedLsp();
            assertNotNull(reportedLsps);
            assertEquals(2, reportedLsps.size());
            return pcc;
        });


        pcRptMsg = createSrPcRpt("1.1.1.2", "sr-path1", 1L, true);
        this.listener.onMessage(this.session, pcRptMsg);
        readDataOperational(getDataBroker(), this.pathComputationClientIId, pcc -> {
            //check updated sr-path
            final List<ReportedLsp> reportedLsps = pcc.getReportedLsp();
            assertNotNull(reportedLsps);
            assertEquals(2, reportedLsps.size());
            for (final ReportedLsp rlsp : reportedLsps) {
                if (rlsp.getName().equals("sr-path1")) {
                    final List<Subobject> subobjects = rlsp.getPath().get(0).getEro().getSubobject();
                    assertEquals(1, subobjects.size());
                    assertEquals("1.1.1.2", ((IpNodeId)((SrEroType)subobjects.get(0)
                        .getSubobjectType()).getNai()).getIpAddress().getIpv4Address().getValue());
                }
            }
            return pcc;
        });
    }

    private static Pcrpt createSrPcRpt(final String nai, final String pathName, final long plspId, final boolean hasLspIdTlv) {
        final TlvsBuilder lspTlvBuilder = new TlvsBuilder();
        if (hasLspIdTlv) {
            lspTlvBuilder.setLspIdentifiers(new LspIdentifiersBuilder().setLspId(new LspId(plspId)).build());
        }
        return new PcrptBuilder().setPcrptMessage(new PcrptMessageBuilder().setReports(Lists.newArrayList(new ReportsBuilder()
            .setLsp(new LspBuilder().setPlspId(new PlspId(plspId)).setRemove(false).setSync(true).setAdministrative(true).setDelegate(true)
                    .setTlvs(lspTlvBuilder
                        .setSymbolicPathName(new SymbolicPathNameBuilder().setPathName(new SymbolicPathName(pathName.getBytes(StandardCharsets.UTF_8))).build()).build()).build())
            .setSrp(new SrpBuilder().setOperationId(new SrpIdNumber(0L)).setTlvs(
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.srp.TlvsBuilder()
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
