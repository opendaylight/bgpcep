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
import static org.junit.Assert.assertNull;
import static org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil.createLsp;
import static org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil.createPath;
import static org.opendaylight.protocol.util.CheckTestUtil.readDataOperational;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.PathComputationClient1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.initiated.rev181109.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.PccSyncState;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;

public class StateSynchronizationAvoidanceProcedureTest extends
    AbstractPCEPSessionTest<PCEPTopologySessionListenerFactory> {

    private PCEPTopologySessionListener listener;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.listener = (PCEPTopologySessionListener) getSessionListener();
    }

    @Test
    public void testNodePersisted() throws ExecutionException, InterruptedException {
        final PCEPSession session = getPCEPSession(getOpen(null), getOpen(null));
        this.listener.onSessionUp(session);
        //report LSP + LSP-DB version number
        final Pcrpt pcRpt = MsgBuilderUtil.createPcRtpMessage(new LspBuilder()
            .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109
                .lsp.object.lsp.TlvsBuilder()
                    .addAugmentation(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller
                        .pcep.sync.optimizations.rev181109.Tlvs1Builder()
                            .setLspDbVersion(new LspDbVersionBuilder().setLspDbVersionValue(Uint64.ONE).build())
                            .build())
                    .build())
            .setPlspId(new PlspId(Uint32.ONE)).setSync(false).setRemove(false).setOperational(OperationalStatus.Active)
            .build(), Optional.of(MsgBuilderUtil.createSrp(Uint32.ONE)), null);
        this.listener.onMessage(session, pcRpt);
        //check topology
        readDataOperational(getDataBroker(), this.pathComputationClientIId.builder()
                .augmentation(PathComputationClient1.class)
                .child(LspDbVersion.class).build(), dbVersion -> {
                assertEquals(1L, dbVersion.getLspDbVersionValue().longValue());
                return dbVersion;
            });

        //drop session
        this.listener.onSessionDown(session, new IllegalStateException());
        readDataOperational(getDataBroker(), TOPO_IID, topology -> {
            assertFalse(topology.getNode().isEmpty());
            return topology;
        });
        //check topology - node is persisted
    }

    @Test
    public void testStateSynchronizationSkipped() throws Exception {
        //session up - sync skipped (LSP-DBs match)
        final LspDbVersion lspDbVersion = new LspDbVersionBuilder().setLspDbVersionValue(Uint64.ONE).build();
        final PCEPSession session = getPCEPSession(getOpen(lspDbVersion), getOpen(lspDbVersion));
        this.listener.onSessionUp(session);
        //check node - synchronized
        readDataOperational(getDataBroker(), this.pathComputationClientIId, pcc -> {
            assertEquals(PccSyncState.Synchronized, pcc.getStateSync());
            return pcc;
        });
    }

    @Test
    public void testStateSynchronizationPerformed() throws Exception {
        PCEPSession session = getPCEPSession(getOpen(null), getOpen(null));
        this.listener.onSessionUp(session);
        //report LSP + LSP-DB version number
        final Pcrpt pcRpt = MsgBuilderUtil.createPcRtpMessage(new LspBuilder()
            .setPlspId(new PlspId(Uint32.ONE))
            .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109
                .lsp.object.lsp.TlvsBuilder()
                    .setLspIdentifiers(new LspIdentifiersBuilder().setLspId(new LspId(Uint32.ONE)).build())
                    .setSymbolicPathName(new SymbolicPathNameBuilder()
                        .setPathName(new SymbolicPathName("test".getBytes())).build())
                    .addAugmentation(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller
                        .pcep.sync.optimizations.rev181109.Tlvs1Builder()
                        .setLspDbVersion(new LspDbVersionBuilder().setLspDbVersionValue(Uint64.ONE).build())
                        .build())
                    .build())
                .setPlspId(new PlspId(Uint32.ONE)).setSync(true).setRemove(false)
                .setOperational(OperationalStatus.Active)
                .build(), Optional.empty(), createPath(Collections.emptyList()));
        this.listener.onMessage(session, pcRpt);
        readDataOperational(getDataBroker(), this.pathComputationClientIId, pcc -> {
            assertFalse(pcc.nonnullReportedLsp().isEmpty());
            return pcc;
        });

        this.listener.onSessionDown(session, new IllegalArgumentException("Simulate Exception"));
        this.listener = (PCEPTopologySessionListener) getSessionListener();

        //session up - expect sync (LSP-DBs do not match)
        final LspDbVersion localDbVersion = new LspDbVersionBuilder()
                .setLspDbVersionValue(Uint64.TWO).build();
        session = getPCEPSession(getOpen(localDbVersion), getOpen(null));
        this.listener.onSessionUp(session);

        readDataOperational(getDataBroker(), this.pathComputationClientIId, pcc -> {
            //check node - not synchronized
            assertEquals(PccSyncState.InitialResync, pcc.getStateSync());
            //check reported LSP - persisted from previous session
            assertFalse(pcc.nonnullReportedLsp().isEmpty());
            return pcc;
        });

        //sync rpt + LSP-DB
        final Pcrpt syncMsg = MsgBuilderUtil.createPcRtpMessage(createLsp(Uint32.ZERO, false, Optional.of(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109
                        .lsp.object.lsp.TlvsBuilder().addAugmentation(new org.opendaylight.yang.gen.v1.urn.opendaylight
                            .params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.Tlvs1Builder()
                            .setLspDbVersion(new LspDbVersionBuilder().setLspDbVersionValue(Uint64.TWO).build())
                            .build()).build()),
                true, false), Optional.empty(),
                createPath(Collections.emptyList()));
        this.listener.onMessage(session, syncMsg);
        readDataOperational(getDataBroker(), this.pathComputationClientIId, pcc -> {
            //check node - synchronized
            assertEquals(PccSyncState.Synchronized, pcc.getStateSync());
            //check reported LSP is empty, LSP state from previous session was purged
            assertNull(pcc.getReportedLsp());
            return pcc;
        });
    }

    private Open getOpen(final LspDbVersion dbVersion) {
        return new OpenBuilder(super.getLocalPref()).setTlvs(new TlvsBuilder().addAugmentation(new Tlvs1Builder()
            .setStateful(new StatefulBuilder()
                .addAugmentation(new Stateful1Builder().setInitiation(Boolean.TRUE).build())
                .addAugmentation(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep
                    .sync.optimizations.rev181109.Stateful1Builder()
                    .setIncludeDbVersion(Boolean.TRUE).build()).build()).build())
            .addAugmentation(new Tlvs3Builder().setLspDbVersion(dbVersion).build()).build()).build();
    }
}
