/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.opendaylight.protocol.util.CheckTestUtil.readDataOperational;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.ero.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.ero.subobject.subobject.type.SrEroType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.explicit.route.object.ero.subobject.subobject.type.SrEroTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.lsp.LspFlagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.sr.subobject.nai.IpNodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.sr.subobject.nai.IpNodeIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.NaiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.PsType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.path.setup.type.tlv.PathSetupTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.pcep.client.attributes.path.computation.client.ReportedLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.pcep.client.attributes.path.computation.client.ReportedLspKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.pcep.client.attributes.path.computation.client.reported.lsp.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev250602.pcep.client.attributes.path.computation.client.reported.lsp.PathKey;
import org.opendaylight.yangtools.yang.common.Uint32;

public class SegmentRoutingTest extends AbstractPCEPSessionTest {
    private AbstractTopologySessionListener listener;
    private PCEPSession session;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        listener = getSessionListener();
        session = getPCEPSession(getLocalPref(), getRemotePref());
    }

    @Test
    public void testOnReportMessage() throws ExecutionException, InterruptedException {
        listener.onSessionUp(session);

        Pcrpt pcRptMsg = createSrPcRpt("1.1.1.1", "sr-path1", Uint32.ONE, true);
        listener.onMessage(session, pcRptMsg);
        readDataOperational(getDataBroker(), pathComputationClientIId, pcc -> {
            //check sr-path
            final Map<ReportedLspKey, ReportedLsp> reportedLsps = pcc.getReportedLsp();
            assertNotNull(reportedLsps);
            assertEquals(1, reportedLsps.size());
            final ReportedLsp lsp = reportedLsps.values().iterator().next();
            assertEquals("sr-path1", lsp.getName());

            final Map<PathKey, Path> paths = lsp.getPath();
            assertNotNull(paths);
            final Path path = paths.values().iterator().next();

            assertEquals(PsType.SrMpls, path.getPathSetupType().getPst());
            final List<Subobject> subobjects = path.getEro().nonnullSubobject();
            assertEquals(1, subobjects.size());
            assertEquals("1.1.1.1", ((IpNodeId)((SrEroType)subobjects.get(0).getSubobjectType())
                .getNai()).getIpAddress().getIpv4AddressNoZone().getValue());
            return pcc;
        });

        pcRptMsg = createSrPcRpt("1.1.1.3", "sr-path2", Uint32.TWO, false);
        listener.onMessage(session, pcRptMsg);
        readDataOperational(getDataBroker(), pathComputationClientIId, pcc -> {
            //check second lsp sr-path
            final Map<ReportedLspKey, ReportedLsp> reportedLsps = pcc.getReportedLsp();
            assertNotNull(reportedLsps);
            assertEquals(2, reportedLsps.size());
            return pcc;
        });


        pcRptMsg = createSrPcRpt("1.1.1.2", "sr-path1", Uint32.ONE, true);
        listener.onMessage(session, pcRptMsg);
        readDataOperational(getDataBroker(), pathComputationClientIId, pcc -> {
            //check updated sr-path
            final Map<ReportedLspKey, ReportedLsp> reportedLsps = pcc.getReportedLsp();
            assertNotNull(reportedLsps);
            assertEquals(2, reportedLsps.size());
            for (final ReportedLsp rlsp : reportedLsps.values()) {
                if (rlsp.getName().equals("sr-path1")) {
                    final List<Subobject> subobjects = rlsp.nonnullPath().values().iterator().next()
                            .getEro().nonnullSubobject();
                    assertEquals(1, subobjects.size());
                    assertEquals("1.1.1.2", ((IpNodeId)((SrEroType)subobjects.get(0)
                        .getSubobjectType()).getNai()).getIpAddress().getIpv4AddressNoZone().getValue());
                }
            }
            return pcc;
        });
    }

    private static Pcrpt createSrPcRpt(final String nai, final String pathName, final Uint32 plspId,
            final boolean hasLspIdTlv) {
        final TlvsBuilder lspTlvBuilder = new TlvsBuilder();
        if (hasLspIdTlv) {
            lspTlvBuilder.setLspIdentifiers(new LspIdentifiersBuilder().setLspId(new LspId(plspId)).build());
        }
        return new PcrptBuilder()
            .setPcrptMessage(new PcrptMessageBuilder()
                .setReports(List.of(new ReportsBuilder()
                    .setLsp(new LspBuilder()
                        .setPlspId(new PlspId(plspId))
                        .setLspFlags(new LspFlagsBuilder()
                            .setRemove(false)
                            .setSync(true)
                            .setAdministrative(true)
                            .setDelegate(true)
                            .build())
                        .setTlvs(lspTlvBuilder
                            .setSymbolicPathName(new SymbolicPathNameBuilder()
                                .setPathName(new SymbolicPathName(pathName.getBytes(StandardCharsets.UTF_8)))
                                .build())
                            .build())
                        .build())
                    .setSrp(new SrpBuilder()
                        .setOperationId(new SrpIdNumber(Uint32.ZERO))
                        .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object
                            .rev250930.srp.object.srp.TlvsBuilder()
                            .setPathSetupType(new PathSetupTypeBuilder().setPst(PsType.SrMpls).build())
                            .build())
                        .build())
                    .setPath(new PathBuilder().setEro(createSrEroObject(nai)).build())
                    .build()))
                .build())
            .build();
    }

    private static Ero createSrEroObject(final String nai) {
        return new EroBuilder()
            .setProcessingRule(false)
            .setIgnore(false)
            .setSubobject(List.of(new SubobjectBuilder()
                .setSubobjectType(new SrEroTypeBuilder()
                    .setCFlag(false)
                    .setMFlag(false)
                    .setNaiType(NaiType.Ipv4NodeId)
                    .setSid(Uint32.valueOf(123456))
                    .setNai(new IpNodeIdBuilder().setIpAddress(new IpAddressNoZone(new Ipv4AddressNoZone(nai))).build())
                    .build())
                .setLoose(false)
                .build()))
            .build();
    }
}
