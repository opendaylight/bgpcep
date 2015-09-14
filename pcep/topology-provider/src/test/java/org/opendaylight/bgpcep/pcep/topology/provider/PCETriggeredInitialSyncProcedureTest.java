/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.topology.provider;

import static org.opendaylight.protocol.pcep.pcc.mock.MsgBuilderUtil.createLsp;
import static org.opendaylight.protocol.pcep.pcc.mock.MsgBuilderUtil.createPath;

import com.google.common.base.Optional;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.pcc.mock.MsgBuilderUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.PccSyncState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.ReportedLsp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;

public class PCETriggeredInitialSyncProcedureTest extends AbstractPCEPSessionTest<Stateful07TopologySessionListenerFactory> {
    private Stateful07TopologySessionListener listener;

    private PCEPSession session;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.listener = (Stateful07TopologySessionListener) getSessionListener();
    }

    @Test
    public void testStateSynchronizationSkipped() throws InterruptedException, ExecutionException {
        //session up - sync skipped (LSP-DBs match)
        final LspDbVersion lspDbVersion = new LspDbVersionBuilder().setLspDbVersionValue(BigInteger.valueOf(1l)).build();
        this.session = getPCEPSession(getOpen(lspDbVersion, Boolean.TRUE), getOpen(lspDbVersion, Boolean.TRUE));
        this.listener.onSessionUp(session);
        //check node - synchronized
        final Topology topo = getTopology().get();
        Assert.assertEquals(PccSyncState.Synchronized, topo.getNode().get(0).getAugmentation(Node1.class).getPathComputationClient().getStateSync());
    }

    /**
     * Test Triggered & Full Sync procedure
     **/
    @Test
    public void testStateSynchronizationPerformed() throws InterruptedException, ExecutionException {
        this.session = getPCEPSession(getOpen(null, Boolean.FALSE), getOpen(null, Boolean.FALSE));
        this.listener.onSessionUp(session);
        //report LSP + LSP-DB version number
        final Pcrpt pcRpt = MsgBuilderUtil.createPcRtpMessage(new LspBuilder().setPlspId(new PlspId(1l)).setTlvs(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.TlvsBuilder().setLspIdentifiers(new LspIdentifiersBuilder().setLspId(new LspId(1l)).build()).setSymbolicPathName(new SymbolicPathNameBuilder().setPathName(new SymbolicPathName("test".getBytes())).build())
                .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs1.class,
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs1Builder().setLspDbVersion(new LspDbVersionBuilder().setLspDbVersionValue(BigInteger.valueOf(1l)).build()).build()).build()).setPlspId(new PlspId(1L)).setSync(true).setRemove(false).setOperational(OperationalStatus.Active).build(), Optional.<Srp>absent(), createPath(Collections.<Subobject>emptyList()));
        this.listener.onMessage(session, pcRpt);
        final Topology topo = getTopology().get();
        final PathComputationClient pcc = topo.getNode().get(0).getAugmentation(Node1.class).getPathComputationClient();
        Assert.assertFalse(pcc.getReportedLsp().isEmpty());
        this.listener.onSessionDown(session, new IllegalArgumentException());
        this.listener = (Stateful07TopologySessionListener) getSessionListener();

        //session up - expect sync (LSP-DBs do not match)
        final LspDbVersion localDbVersion = new LspDbVersionBuilder().setLspDbVersionValue(BigInteger.valueOf(2l)).build();
        this.session = getPCEPSession(getOpen(localDbVersion, Boolean.FALSE), getOpen(null, Boolean.FALSE));
        this.listener.onSessionUp(session);

        final Topology topo2 = getTopology().get();
        final PathComputationClient pcc2 = topo2.getNode().get(0).getAugmentation(Node1.class).getPathComputationClient();
        //check node - not synchronized
        Assert.assertEquals(PccSyncState.TriggeredInitialSync, pcc2.getStateSync());
        //check reported LSP - persisted from previous session
        Assert.assertFalse(pcc2.getReportedLsp().isEmpty());
        //sync rpt + LSP-DB
        final Pcrpt syncMsg = MsgBuilderUtil.createPcRtpMessage(createLsp(0, false, Optional.of(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.TlvsBuilder().addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs1.class,
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs1Builder().setLspDbVersion(new LspDbVersionBuilder().setLspDbVersionValue(BigInteger.valueOf(2l)).build()).build()).build()), true, false), Optional.<Srp>absent(),
            createPath(Collections.<Subobject>emptyList()));
        this.listener.onMessage(session, syncMsg);
        final Topology topo3 = getTopology().get();
        final PathComputationClient pcc3 = topo3.getNode().get(0).getAugmentation(Node1.class).getPathComputationClient();
        //check node - synchronized
        Assert.assertEquals(PccSyncState.Synchronized, pcc3.getStateSync());
        //check reported LSP is empty, LSP state from previous session was purged
        Assert.assertTrue(pcc3.getReportedLsp().isEmpty());
    }

    /**
     * Test PCE Triggered & Incremental Sync procedure
     **/
    @Test
    public void testPCETriggeredStateSynchronizationPerformed() throws InterruptedException, ExecutionException {
        this.session = getPCEPSession(getOpen(null, Boolean.TRUE), getOpen(null, Boolean.TRUE));
        this.listener.onSessionUp(session);
        //report LSP + LSP-DB version number
        final Pcrpt pcRpt = MsgBuilderUtil.createPcRtpMessage(new LspBuilder().setPlspId(new PlspId(1l)).setTlvs(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.
                TlvsBuilder().setLspIdentifiers(new LspIdentifiersBuilder().setLspId(new LspId(1l)).build()).setSymbolicPathName(new SymbolicPathNameBuilder().setPathName(new SymbolicPathName("test".getBytes())).build())
                .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs1.class,
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs1Builder().setLspDbVersion(new LspDbVersionBuilder().setLspDbVersionValue(BigInteger.valueOf(1l)).build()).build()).build()).setPlspId(new PlspId(1L)).setSync(true).setRemove(false).setOperational(OperationalStatus.Active).build(), Optional.<Srp>absent(), createPath(Collections.<Subobject>emptyList()));
        this.listener.onMessage(session, pcRpt);

        final Topology topo = getTopology().get();
        final PathComputationClient pcc = topo.getNode().get(0).getAugmentation(Node1.class).getPathComputationClient();
        final List<ReportedLsp> reportedLspPcc = pcc.getReportedLsp();
        Assert.assertFalse(reportedLspPcc.isEmpty());
        this.listener.onSessionDown(session, new IllegalArgumentException());
        this.listener = (Stateful07TopologySessionListener) getSessionListener();

        //session up - expect sync (LSP-DBs do not match)
        final LspDbVersion localDbVersion = new LspDbVersionBuilder().setLspDbVersionValue(BigInteger.valueOf(2l)).build();
        this.session = getPCEPSession(getOpen(localDbVersion, Boolean.TRUE), getOpen(null, Boolean.TRUE));
        this.listener.onSessionUp(session);

        final Topology topo2 = getTopology().get();
        final PathComputationClient pcc2 = topo2.getNode().get(0).getAugmentation(Node1.class).getPathComputationClient();
        //check node - not synchronized
        Assert.assertEquals(PccSyncState.TriggeredInitialSync, pcc2.getStateSync());
        //check reported LSP - persisted from previous session
        final List<ReportedLsp> reportedLspPcc2 = pcc2.getReportedLsp();
        Assert.assertFalse(reportedLspPcc2.isEmpty());

        //report LSP2 + LSP-DB version number 2
        final Pcrpt pcRpt2 = MsgBuilderUtil.createPcRtpMessage(new LspBuilder().setPlspId(new PlspId(2l)).setTlvs(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.
                TlvsBuilder().setLspIdentifiers(new LspIdentifiersBuilder().setLspId(new LspId(1l)).build())
                .setSymbolicPathName(new SymbolicPathNameBuilder().setPathName(new SymbolicPathName("testsecond".getBytes())).build())
                .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs1.class, new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync
                    .optimizations.rev150714.Tlvs1Builder().setLspDbVersion(new LspDbVersionBuilder().setLspDbVersionValue(BigInteger.valueOf(2l)).build()).build()).build()).setPlspId(new PlspId(2L)
        ).setSync(true).setRemove(false).setOperational(OperationalStatus.Active).build(), Optional.<Srp>absent(), createPath(Collections.<Subobject>emptyList()));
        this.listener.onMessage(session, pcRpt2);

        final Topology topo3 = getTopology().get();
        final PathComputationClient pcc3 = topo3.getNode().get(0).getAugmentation(Node1.class)
            .getPathComputationClient();

        //check node - synchronized
        Assert.assertEquals(PccSyncState.TriggeredInitialSync, pcc3.getStateSync());
        //check reported LSP is empty, LSP state from previous session was purged
        final List<ReportedLsp> reportedLspPcc3 = pcc3.getReportedLsp();
        Assert.assertEquals(reportedLspPcc2.size() + 1, reportedLspPcc3.size());

        //sync rpt + LSP-DB
        final Pcrpt syncMsg = MsgBuilderUtil.createPcRtpMessage(createLsp(0, false, Optional.of(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.TlvsBuilder().addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs1.class,
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync
                        .optimizations.rev150714.Tlvs1Builder().setLspDbVersion(new LspDbVersionBuilder().setLspDbVersionValue(BigInteger.valueOf(3l)).build()).build()).build()), true, false), Optional.<Srp>absent(),
            createPath(Collections.<Subobject>emptyList()));
        this.listener.onMessage(session, syncMsg);
        final Topology topo4 = getTopology().get();
        final PathComputationClient pcc4 = topo4.getNode().get(0).getAugmentation(Node1.class)
            .getPathComputationClient();
        //check node - synchronized
        Assert.assertEquals(PccSyncState.Synchronized, pcc4.getStateSync());
        //check reported LSP is empty, LSP state from previous session was purged
        final List<ReportedLsp> reportedLspPcc4 = pcc4.getReportedLsp();
        Assert.assertEquals(reportedLspPcc4.size(), reportedLspPcc3.size());
    }

    protected Open getOpen(final LspDbVersion dbVersion, final boolean incremental) {
        return new OpenBuilder(super.getLocalPref()).setTlvs(new TlvsBuilder().addAugmentation(Tlvs1.class, new Tlvs1Builder().setStateful(new StatefulBuilder()
            .addAugmentation(Stateful1.class, new Stateful1Builder().setInitiation(Boolean.TRUE).build())
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Stateful1.class, new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Stateful1Builder()
                .setIncludeDbVersion(Boolean.TRUE)
                .setTriggeredInitialSync(Boolean.TRUE)
                .setDeltaLspSyncCapability(incremental).build())
            .build()).build()).addAugmentation(Tlvs3.class, new Tlvs3Builder().setLspDbVersion(dbVersion).build()).build()).build();
    }
}
