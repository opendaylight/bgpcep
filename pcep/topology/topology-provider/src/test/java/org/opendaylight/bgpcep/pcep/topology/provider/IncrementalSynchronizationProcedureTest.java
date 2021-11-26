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
import static org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil.createLsp;
import static org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil.createPath;
import static org.opendaylight.protocol.util.CheckTestUtil.readDataOperational;

import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.PccSyncState;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;

public class IncrementalSynchronizationProcedureTest extends AbstractPCEPSessionTest {
    private PCEPTopologySessionListener listener;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        listener = getSessionListener();
    }

    @Test
    public void testStateSynchronizationPerformed() throws Exception {
        PCEPSession session = getPCEPSession(getOpen(null), getOpen(null));
        listener.onSessionUp(session);
        //report LSP + LSP-DB version number
        final Pcrpt pcRpt = getPcrpt(Uint32.ONE, "test");
        listener.onMessage(session, pcRpt);
        readDataOperational(getDataBroker(), pathComputationClientIId, pcc -> {
            assertFalse(pcc.nonnullReportedLsp().isEmpty());
            return pcc;
        });

        listener.onSessionDown(session, new IllegalArgumentException());
        listener = getSessionListener();

        //session up - expect sync (LSP-DBs do not match)
        final LspDbVersion localDbVersion = new LspDbVersionBuilder()
                .setLspDbVersionValue(Uint64.TWO).build();
        session = getPCEPSession(getOpen(localDbVersion), getOpen(null));
        listener.onSessionUp(session);
        readDataOperational(getDataBroker(), pathComputationClientIId, pcc -> {
            //check node - IncrementalSync state
            assertEquals(PccSyncState.IncrementalSync, pcc.getStateSync());
            //check reported LSP - persisted from previous session
            assertFalse(pcc.nonnullReportedLsp().isEmpty());
            return pcc;
        });

        //report LSP2 + LSP-DB version number 2
        final Pcrpt pcRpt2 = getPcrpt(Uint32.TWO, "testsecond");
        listener.onMessage(session, pcRpt2);
        readDataOperational(getDataBroker(), pathComputationClientIId, pcc -> {
            //check node - synchronized
            assertEquals(PccSyncState.IncrementalSync, pcc.getStateSync());
            //check reported LSP is not empty
            assertEquals(2, pcc.nonnullReportedLsp().size());
            return pcc;
        });

        //sync rpt + LSP-DB
        final Pcrpt syncMsg = getSyncPcrt();
        listener.onMessage(session, syncMsg);
        readDataOperational(getDataBroker(), pathComputationClientIId, pcc -> {
            //check node - synchronized
            assertEquals(PccSyncState.Synchronized, pcc.getStateSync());
            //check reported LSP is empty, LSP state from previous session was purged
            assertEquals(2, pcc.nonnullReportedLsp().size());
            return pcc;
        });

        //report LSP3 + LSP-DB version number 4
        final Pcrpt pcRpt3 = getPcrpt(Uint32.valueOf(3), "testthird");
        listener.onMessage(session, pcRpt3);
        readDataOperational(getDataBroker(), pathComputationClientIId, pcc -> {
            //check node - synchronized
            assertEquals(PccSyncState.Synchronized, pcc.getStateSync());
            assertEquals(3,pcc.nonnullReportedLsp().size());
            return pcc;
        });
    }

    private Open getOpen(final LspDbVersion dbVersion) {
        return new OpenBuilder(super.getLocalPref())
                .setTlvs(new TlvsBuilder()
                    .addAugmentation(new Tlvs1Builder()
                        .setStateful(new StatefulBuilder()
                            .addAugmentation(new Stateful1Builder().setInitiation(Boolean.TRUE).build())
                            .addAugmentation(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                                .controller.pcep.sync.optimizations.rev200720.Stateful1Builder()
                                .setIncludeDbVersion(Boolean.TRUE).setDeltaLspSyncCapability(Boolean.TRUE)
                                .build())
                            .build())
                        .build())
                    .addAugmentation(new Tlvs3Builder().setLspDbVersion(dbVersion).build())
                    .build())
                .build();
    }

    private static Pcrpt getPcrpt(final Uint32 val, final String pathname) {
        return MsgBuilderUtil.createPcRtpMessage(new LspBuilder().setPlspId(new PlspId(val)).setTlvs(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp
                    .object.lsp.TlvsBuilder().setLspIdentifiers(new LspIdentifiersBuilder()
                    .setLspId(new LspId(val)).build())
                .setSymbolicPathName(new SymbolicPathNameBuilder().setPathName(new SymbolicPathName(
                    pathname.getBytes())).build()).addAugmentation(new org.opendaylight.yang.gen.v1.urn.opendaylight
                        .params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs1Builder()
                        .setLspDbVersion(new LspDbVersionBuilder().setLspDbVersionValue(Uint64.valueOf(val)).build())
                        .build()).build()).setPlspId(new PlspId(val)
        ).setSync(true).setRemove(false).setOperational(OperationalStatus.Active).build(), Optional.empty(),
            createPath(Collections.emptyList()));
    }

    private static Pcrpt getSyncPcrt() {
        return MsgBuilderUtil.createPcRtpMessage(createLsp(Uint32.ZERO, false, Optional.of(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720
                        .lsp.object.lsp.TlvsBuilder().addAugmentation(new org.opendaylight.yang.gen.v1.urn.opendaylight
                            .params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs1Builder()
                            .setLspDbVersion(new LspDbVersionBuilder().setLspDbVersionValue(Uint64.valueOf(3L)).build())
                            .build()).build()),
                true, false), Optional.empty(),
                createPath(Collections.emptyList()));
    }
}
