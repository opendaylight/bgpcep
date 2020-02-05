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
import static org.junit.Assert.assertTrue;
import static org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil.createLsp;
import static org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil.createPath;
import static org.opendaylight.protocol.util.CheckTestUtil.readDataOperational;

import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev181109.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev181109.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.PccSyncState;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;

public class PCETriggeredInitialSyncProcedureTest
        extends AbstractPCEPSessionTest<Stateful07TopologySessionListenerFactory> {
    private Stateful07TopologySessionListener listener;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.listener = (Stateful07TopologySessionListener) getSessionListener();
    }

    /**
     * Test Triggered Initial Sync procedure.
     **/
    @Test
    public void testPcepTriggeredInitialSyncPerformed() throws Exception {
        this.listener = (Stateful07TopologySessionListener) getSessionListener();

        //session up - expect triggered sync (LSP-DBs do not match)
        final LspDbVersion localDbVersion = new LspDbVersionBuilder()
                .setLspDbVersionValue(Uint64.ONE).build();
        final LspDbVersion localDbVersion2 = new LspDbVersionBuilder()
                .setLspDbVersionValue(Uint64.TWO).build();
        final PCEPSession session = getPCEPSession(getOpen(localDbVersion, Boolean.FALSE),
                getOpen(localDbVersion2, Boolean.FALSE));
        this.listener.onSessionUp(session);

        readDataOperational(getDataBroker(), this.pathComputationClientIId, pcc -> {
            //check node - not synchronized and TriggeredInitialSync state
            assertEquals(PccSyncState.TriggeredInitialSync, pcc.getStateSync());
            return pcc;
        });

        //sync rpt + LSP-DB
        final Pcrpt syncMsg = getsyncMsg();
        this.listener.onMessage(session, syncMsg);
        readDataOperational(getDataBroker(), this.pathComputationClientIId, pcc -> {
            //check node - synchronized
            assertEquals(PccSyncState.Synchronized, pcc.getStateSync());
            //check reported LSP is empty, LSP state from previous session was purged
            assertTrue(pcc.getReportedLsp().isEmpty());
            return pcc;
        });

        //report LSP + LSP-DB version number
        final Pcrpt pcRpt = getPcrpt();
        this.listener.onMessage(session, pcRpt);

        readDataOperational(getDataBroker(), this.pathComputationClientIId, pcc -> {
            assertFalse(pcc.getReportedLsp().isEmpty());
            return pcc;
        });

    }

    private Open getOpen(final LspDbVersion dbVersion, final boolean incremental) {
        return new OpenBuilder(super.getLocalPref()).setTlvs(new TlvsBuilder().addAugmentation(Tlvs1.class,
                new Tlvs1Builder().setStateful(new StatefulBuilder()
                        .addAugmentation(Stateful1.class, new Stateful1Builder().setInitiation(Boolean.TRUE).build())
                        .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller
                                        .pcep.sync.optimizations.rev181109.Stateful1.class,
                                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep
                                        .sync.optimizations.rev181109.Stateful1Builder()
                                        .setIncludeDbVersion(Boolean.TRUE)
                                        .setTriggeredInitialSync(Boolean.TRUE)
                                        .setDeltaLspSyncCapability(incremental).build()).build()).build())
                .addAugmentation(Tlvs3.class, new Tlvs3Builder().setLspDbVersion(dbVersion).build())
                .build()).build();
    }

    private static Pcrpt getsyncMsg() {
        return MsgBuilderUtil.createPcRtpMessage(createLsp(Uint32.ZERO, false, Optional.of(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp
                    .object.lsp.TlvsBuilder().addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params
                        .xml.ns.yang.controller.pcep.sync.optimizations.rev181109.Tlvs1.class, new org.opendaylight
                        .yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109
                        .Tlvs1Builder().setLspDbVersion(new LspDbVersionBuilder().setLspDbVersionValue(Uint64
                        .valueOf(2L)).build()).build()).build()), true, false), Optional.empty(),
                        createPath(Collections.emptyList()));
    }

    private static Pcrpt getPcrpt() {
        return MsgBuilderUtil.createPcRtpMessage(new LspBuilder().setPlspId(new PlspId(Uint32.ONE)).setTlvs(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.lsp
                        .object.lsp.TlvsBuilder().setLspIdentifiers(new LspIdentifiersBuilder()
                        .setLspId(new LspId(Uint32.ONE)).build()).setSymbolicPathName(new SymbolicPathNameBuilder()
                        .setPathName(new SymbolicPathName("test".getBytes())).build()).addAugmentation(org.opendaylight
                        .yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev181109
                        .Tlvs1.class, new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller
                        .pcep.sync.optimizations.rev181109.Tlvs1Builder().setLspDbVersion(new LspDbVersionBuilder()
                        .setLspDbVersionValue(Uint64.valueOf(3L)).build())
                        .build()).build()).setPlspId(new PlspId(Uint32.ONE)).setSync(true).setRemove(false)
                .setOperational(OperationalStatus.Active).build(), Optional.empty(),
                createPath(Collections.emptyList()));
    }
}
