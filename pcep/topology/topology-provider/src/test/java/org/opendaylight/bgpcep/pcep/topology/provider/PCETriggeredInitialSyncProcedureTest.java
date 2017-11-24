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
import static org.opendaylight.protocol.util.CheckUtil.readDataOperational;

import com.google.common.base.Optional;
import java.math.BigInteger;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.pcc.mock.spi.MsgBuilderUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.PccSyncState;

public class PCETriggeredInitialSyncProcedureTest extends AbstractPCEPSessionTest<Stateful07TopologySessionListenerFactory> {
    private Stateful07TopologySessionListener listener;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.listener = (Stateful07TopologySessionListener) getSessionListener();
    }

    /**
     * Test Triggered Initial Sync procedure
     **/
    @Test
    public void testPcepTriggeredInitialSyncPerformed() throws Exception {
        this.listener = (Stateful07TopologySessionListener) getSessionListener();

        //session up - expect triggered sync (LSP-DBs do not match)
        final LspDbVersion localDbVersion = new LspDbVersionBuilder().setLspDbVersionValue(BigInteger.ONE).build();
        final LspDbVersion localDbVersion2 = new LspDbVersionBuilder().setLspDbVersionValue(BigInteger.valueOf(2L)).build();
        final PCEPSession session = getPCEPSession(getOpen(localDbVersion, Boolean.FALSE), getOpen(localDbVersion2, Boolean.FALSE));
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
        return new OpenBuilder(super.getLocalPref()).setTlvs(new TlvsBuilder().addAugmentation(Tlvs1.class, new Tlvs1Builder().setStateful(new StatefulBuilder()
            .addAugmentation(Stateful1.class, new Stateful1Builder().setInitiation(Boolean.TRUE).build())
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync
                .optimizations.rev171025.Stateful1.class, new org.opendaylight.yang.gen.v1.urn.opendaylight.params
                .xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Stateful1Builder().setIncludeDbVersion
                (Boolean.TRUE).setTriggeredInitialSync(Boolean.TRUE).setDeltaLspSyncCapability(incremental).build())
            .build()).build()).addAugmentation(Tlvs3.class, new Tlvs3Builder().setLspDbVersion(dbVersion).build()).build()).build();
    }

    private Pcrpt getsyncMsg() {
        return MsgBuilderUtil.createPcRtpMessage(createLsp(0, false, Optional.of(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp
                    .object.lsp.TlvsBuilder().addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params
                        .xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs1.class, new org.opendaylight
                        .yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025
                        .Tlvs1Builder().setLspDbVersion(new LspDbVersionBuilder().setLspDbVersionValue(BigInteger
                        .valueOf(2L)).build()).build()).build()), true, false), Optional.absent(),
                        createPath(Collections.emptyList()));
    }

    private Pcrpt getPcrpt() {
        return MsgBuilderUtil.createPcRtpMessage(new LspBuilder().setPlspId(new PlspId(1L)).setTlvs(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp
                .object.lsp.TlvsBuilder().setLspIdentifiers(new LspIdentifiersBuilder().setLspId(new LspId(1L)).build
                ()).setSymbolicPathName(new SymbolicPathNameBuilder().setPathName(new SymbolicPathName("test"
                .getBytes())).build()).addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                .yang.controller.pcep.sync.optimizations.rev171025.Tlvs1.class, new org.opendaylight.yang.gen.v1
                .urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs1Builder()
                .setLspDbVersion(new LspDbVersionBuilder().setLspDbVersionValue(BigInteger.valueOf(3L)).build())
                .build()).build()).setPlspId(new PlspId(1L)).setSync(true).setRemove(false).setOperational
            (OperationalStatus.Active).build(), Optional.absent(), createPath(Collections.emptyList()));
    }
}
