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

import java.util.List;
import java.util.Optional;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.PccSyncState;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;

public class PCETriggeredInitialSyncProcedureTest extends AbstractPCEPSessionTest {
    /**
     * Test Triggered Initial Sync procedure.
     **/
    @Test
    public void testPcepTriggeredInitialSyncPerformed() throws Exception {
        final PCEPTopologySessionListener listener = getSessionListener();

        //session up - expect triggered sync (LSP-DBs do not match)
        final LspDbVersion localDbVersion = new LspDbVersionBuilder()
                .setLspDbVersionValue(Uint64.ONE).build();
        final LspDbVersion localDbVersion2 = new LspDbVersionBuilder()
                .setLspDbVersionValue(Uint64.TWO).build();
        final PCEPSession session = getPCEPSession(getOpen(localDbVersion, false),
                getOpen(localDbVersion2, false));
        listener.onSessionUp(session);

        readDataOperational(getDataBroker(), pathComputationClientIId, pcc -> {
            //check node - not synchronized and TriggeredInitialSync state
            assertEquals(PccSyncState.TriggeredInitialSync, pcc.getStateSync());
            return pcc;
        });

        //sync rpt + LSP-DB
        final Pcrpt syncMsg = getsyncMsg();
        listener.onMessage(session, syncMsg);
        readDataOperational(getDataBroker(), pathComputationClientIId, pcc -> {
            //check node - synchronized
            assertEquals(PccSyncState.Synchronized, pcc.getStateSync());
            //check reported LSP is empty, LSP state from previous session was purged
            assertNull(pcc.getReportedLsp());
            return pcc;
        });

        //report LSP + LSP-DB version number
        final Pcrpt pcRpt = getPcrpt();
        listener.onMessage(session, pcRpt);

        readDataOperational(getDataBroker(), pathComputationClientIId, pcc -> {
            assertFalse(pcc.nonnullReportedLsp().isEmpty());
            return pcc;
        });

    }

    private Open getOpen(final LspDbVersion dbVersion, final boolean incremental) {
        return new OpenBuilder(super.getLocalPref())
                .setTlvs(new TlvsBuilder()
                    .addAugmentation(new Tlvs1Builder()
                        .setStateful(new StatefulBuilder()
                            .addAugmentation(new Stateful1Builder().setInitiation(Boolean.TRUE).build())
                            .addAugmentation(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                                .controller.pcep.sync.optimizations.rev200720.Stateful1Builder()
                                .setIncludeDbVersion(Boolean.TRUE)
                                .setTriggeredInitialSync(Boolean.TRUE)
                                .setDeltaLspSyncCapability(incremental).build()).build()).build())
                    .addAugmentation(new Tlvs3Builder().setLspDbVersion(dbVersion).build())
                    .build())
                .build();
    }

    private static Pcrpt getsyncMsg() {
        return MsgBuilderUtil.createPcRtpMessage(createLsp(Uint32.ZERO, false, Optional.of(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp
                    .object.lsp.TlvsBuilder().addAugmentation(new org.opendaylight.yang.gen.v1.urn.opendaylight.params
                        .xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs1Builder()
                        .setLspDbVersion(new LspDbVersionBuilder().setLspDbVersionValue(Uint64.TWO).build()).build())
                    .build()), true, false), Optional.empty(), createPath(List.of()));
    }

    private static Pcrpt getPcrpt() {
        return MsgBuilderUtil.createPcRtpMessage(new LspBuilder().setPlspId(new PlspId(Uint32.ONE)).setTlvs(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.lsp
                        .object.lsp.TlvsBuilder()
                        .setLspIdentifiers(new LspIdentifiersBuilder().setLspId(new LspId(Uint32.ONE)).build())
                        .setSymbolicPathName(new SymbolicPathNameBuilder()
                                .setPathName(new SymbolicPathName("test".getBytes()))
                                .build())
                        .addAugmentation(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller
                            .pcep.sync.optimizations.rev200720.Tlvs1Builder()
                                .setLspDbVersion(new LspDbVersionBuilder()
                                    .setLspDbVersionValue(Uint64.valueOf(3L))
                                    .build())
                                .build())
                        .build())
            .setPlspId(new PlspId(Uint32.ONE)).setSync(true).setRemove(false)
            .setOperational(OperationalStatus.Active).build(), Optional.empty(), createPath(List.of()));
    }
}
