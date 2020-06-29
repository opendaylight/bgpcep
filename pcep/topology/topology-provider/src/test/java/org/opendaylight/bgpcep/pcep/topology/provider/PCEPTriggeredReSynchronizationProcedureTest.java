/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil.createLsp;
import static org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil.createPath;
import static org.opendaylight.protocol.util.CheckTestUtil.readDataOperational;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev181109.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.PccSyncState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.TriggerSyncInputBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;

public class PCEPTriggeredReSynchronizationProcedureTest
        extends AbstractPCEPSessionTest<Stateful07TopologySessionListenerFactory> {
    private Stateful07TopologySessionListener listener;

    private PCEPSession session;
    private final LspDbVersion lspDbVersion = new LspDbVersionBuilder().setLspDbVersionValue(Uint64.ONE).build();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.listener = (Stateful07TopologySessionListener) getSessionListener();
    }

    @Test
    public void testTriggeredResynchronization() throws Exception {
        //session up - sync skipped (LSP-DBs match)
        this.session = getPCEPSession(getOpen(), getOpen());
        this.listener.onSessionUp(this.session);

        //report LSP + LSP-DB version number
        final Pcrpt pcRpt = getPcrt();
        this.listener.onMessage(this.session, pcRpt);

        readDataOperational(getDataBroker(), this.pathComputationClientIId, pcc -> {
            assertEquals(PccSyncState.Synchronized, pcc.getStateSync());
            assertFalse(pcc.nonnullReportedLsp().isEmpty());
            return pcc;
        });

        //PCEP Trigger Full Resync
        this.listener.triggerSync(new TriggerSyncInputBuilder().setNode(this.nodeId).build());
        readDataOperational(getDataBroker(), this.pathComputationClientIId, pcc -> {
            assertEquals(PccSyncState.PcepTriggeredResync, pcc.getStateSync());
            return pcc;
        });

        //end of sync
        final Pcrpt syncMsg = getSyncMsg();
        this.listener.onMessage(this.session, syncMsg);
        readDataOperational(getDataBroker(), this.pathComputationClientIId, pcc -> {
            //check node - synchronized
            assertEquals(PccSyncState.Synchronized, pcc.getStateSync());
            return pcc;
        });

        this.listener.onMessage(this.session, pcRpt);
        readDataOperational(getDataBroker(), this.pathComputationClientIId, pcc -> {
            final Map<?, ?> lsp = pcc.getReportedLsp();
            assertNotNull(lsp);
            assertEquals(1, lsp.size());
            return pcc;
        });

        //Trigger Full Resync
        this.listener.triggerSync(new TriggerSyncInputBuilder().setNode(this.nodeId).build());
        this.listener.onMessage(this.session, pcRpt);
        //end of sync
        this.listener.onMessage(this.session, syncMsg);
        readDataOperational(getDataBroker(), this.pathComputationClientIId, pcc -> {
            //check node - synchronized
            assertEquals(PccSyncState.Synchronized, pcc.getStateSync());
            //check reported LSP is not empty, Stale LSP state were purged
            assertEquals(1, pcc.getReportedLsp().size());
            return pcc;
        });
    }

    @Test
    public void testTriggeredResynchronizationLsp() throws Exception {
        //session up - sync skipped (LSP-DBs match)

        this.session = getPCEPSession(getOpen(), getOpen());
        this.listener.onSessionUp(this.session);

        //report LSP + LSP-DB version number
        final Pcrpt pcRpt = getPcrt();
        this.listener.onMessage(this.session, pcRpt);
        readDataOperational(getDataBroker(), this.pathComputationClientIId, pcc -> {
            assertEquals(PccSyncState.Synchronized, pcc.getStateSync());
            assertFalse(pcc.nonnullReportedLsp().isEmpty());
            return pcc;
        });

        //Trigger Full Resync
        this.listener.triggerSync(new TriggerSyncInputBuilder().setNode(this.nodeId).setName("test").build());
        readDataOperational(getDataBroker(), this.pathComputationClientIId, pcc -> {
            assertEquals(PccSyncState.PcepTriggeredResync, pcc.getStateSync());
            assertFalse(pcc.nonnullReportedLsp().isEmpty());
            return pcc;
        });

        this.listener.onMessage(this.session, pcRpt);
        readDataOperational(getDataBroker(), this.pathComputationClientIId, pcc -> {
            assertFalse(pcc.nonnullReportedLsp().isEmpty());
            return pcc;
        });

        //sync rpt + LSP-DB
        final Pcrpt syncMsg = getSyncMsg();
        this.listener.onMessage(this.session, syncMsg);
        readDataOperational(getDataBroker(), this.pathComputationClientIId, pcc -> {
            //check node - synchronized
            assertEquals(PccSyncState.Synchronized, pcc.getStateSync());
            //check reported LSP
            assertEquals(1, pcc.nonnullReportedLsp().size());
            return pcc;
        });

        //Trigger Full Resync
        this.listener.triggerSync(new TriggerSyncInputBuilder().setNode(this.nodeId).setName("test").build());
        this.listener.onMessage(this.session, syncMsg);
        readDataOperational(getDataBroker(), this.pathComputationClientIId, pcc -> {
            //check node - synchronized
            assertEquals(PccSyncState.Synchronized, pcc.getStateSync());
            //check reported LSP
            assertNull(pcc.getReportedLsp());
            return pcc;
        });
    }

    private Open getOpen() {
        return new OpenBuilder(super.getLocalPref()).setTlvs(new TlvsBuilder().addAugmentation(new Tlvs1Builder()
            .setStateful(new StatefulBuilder()
                .addAugmentation(new Stateful1Builder().setInitiation(Boolean.TRUE).build())
                .addAugmentation(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep
                    .sync.optimizations.rev181109.Stateful1Builder()
                        .setIncludeDbVersion(Boolean.TRUE).setTriggeredResync(Boolean.TRUE)
                        .build())
                .build())
            .build()).addAugmentation(new Tlvs3Builder().setLspDbVersion(this.lspDbVersion).build()).build()).build();
    }

    private static Pcrpt getSyncMsg() {
        final SrpBuilder srpBuilder = new SrpBuilder();
        // not sue whether use 0 instead of nextRequest() or do not insert srp == SRP-ID-number = 0
        srpBuilder.setOperationId(new SrpIdNumber(Uint32.ONE));
        return MsgBuilderUtil.createPcRtpMessage(createLsp(Uint32.ZERO, false, Optional.of(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp
                        .object.lsp.TlvsBuilder().addAugmentation(new org.opendaylight.yang.gen.v1.urn.opendaylight
                            .params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.Tlvs1Builder()
                            .setLspDbVersion(new LspDbVersionBuilder().setLspDbVersionValue(Uint64.valueOf(3L))
                                .build()).build()).build()), true, false),
            Optional.of(srpBuilder.build()), createPath(Collections.emptyList()));
    }

    private static Pcrpt getPcrt() {
        return MsgBuilderUtil.createPcRtpMessage(new LspBuilder().setPlspId(new PlspId(Uint32.ONE)).setTlvs(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp
                        .object.lsp.TlvsBuilder().setLspIdentifiers(new LspIdentifiersBuilder()
                        .setLspId(new LspId(Uint32.ONE)).build()).setSymbolicPathName(
                    new SymbolicPathNameBuilder().setPathName(new SymbolicPathName("test".getBytes())).build())
                    .addAugmentation(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller
                        .pcep.sync.optimizations.rev181109.Tlvs1Builder().setLspDbVersion(new LspDbVersionBuilder()
                                .setLspDbVersionValue(Uint64.ONE).build()).build()).build())
                .setPlspId(new PlspId(Uint32.ONE)).setSync(true).setRemove(false)
                        .setOperational(OperationalStatus.Active).build(), Optional.empty(),
            createPath(Collections.emptyList()));
    }
}
