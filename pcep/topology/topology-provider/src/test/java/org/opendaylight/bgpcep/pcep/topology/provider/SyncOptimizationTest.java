/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.TlvsBuilder;
import org.opendaylight.yangtools.yang.common.Uint64;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class SyncOptimizationTest {
    @Mock
    private PCEPSession pcepSession;

    @Test
    public void testDoesLspDbMatchPositive() {
        final Tlvs tlvs = createTlvs(1L, false, false);
        doReturn(tlvs).when(pcepSession).getLocalTlvs();
        doReturn(tlvs).when(pcepSession).getRemoteTlvs();
        final SyncOptimization syncOpt = new SyncOptimization(pcepSession);
        assertTrue(syncOpt.doesLspDbMatch());
    }

    @Test
    public void testDoesLspDbMatchNegative() {
        final Tlvs localTlvs = createTlvs(1L, false, false);
        final Tlvs remoteTlvs = createTlvs(2L, false, false);
        doReturn(localTlvs).when(pcepSession).getLocalTlvs();
        doReturn(remoteTlvs).when(pcepSession).getRemoteTlvs();
        final SyncOptimization syncOpt = new SyncOptimization(pcepSession);
        assertFalse(syncOpt.doesLspDbMatch());
    }

    @Test
    public void testIsSyncAvoidanceEnabledPositive() {
        final Tlvs tlvs = createTlvs(1L, true, false);
        doReturn(tlvs).when(pcepSession).getLocalTlvs();
        doReturn(tlvs).when(pcepSession).getRemoteTlvs();
        final SyncOptimization syncOpt = new SyncOptimization(pcepSession);
        assertTrue(syncOpt.isSyncAvoidanceEnabled());
    }

    @Test
    public void testIsSyncAvoidanceEnabledNegative() {
        final Tlvs localTlvs = createTlvs(1L, true, false);
        final Tlvs remoteTlvs = createTlvs(2L, false, false);
        doReturn(localTlvs).when(pcepSession).getLocalTlvs();
        doReturn(remoteTlvs).when(pcepSession).getRemoteTlvs();
        final SyncOptimization syncOpt = new SyncOptimization(pcepSession);
        assertFalse(syncOpt.isSyncAvoidanceEnabled());
    }

    @Test
    public void testIsDeltaSyncEnabledPositive() {
        final Tlvs tlvs = createTlvs(1L, true, true);
        doReturn(tlvs).when(pcepSession).getLocalTlvs();
        doReturn(tlvs).when(pcepSession).getRemoteTlvs();
        final SyncOptimization syncOpt = new SyncOptimization(pcepSession);
        assertTrue(syncOpt.isDeltaSyncEnabled());
    }

    @Test
    public void testIsDeltaSyncEnabledNegative() {
        final Tlvs localTlvs = createTlvs(1L, true, true);
        final Tlvs remoteTlvs = createTlvs(2L, false, false);
        doReturn(localTlvs).when(pcepSession).getLocalTlvs();
        doReturn(remoteTlvs).when(pcepSession).getRemoteTlvs();
        final SyncOptimization syncOpt = new SyncOptimization(pcepSession);
        assertFalse(syncOpt.isDeltaSyncEnabled());
    }

    @Test
    public void testIsDbVersionPresentPositive() {
        final Tlvs localTlvs = createTlvs(null, false, false);
        final Tlvs remoteTlvs = createTlvs(2L, false, false);
        doReturn(localTlvs).when(pcepSession).getLocalTlvs();
        doReturn(remoteTlvs).when(pcepSession).getRemoteTlvs();
        final SyncOptimization syncOpt = new SyncOptimization(pcepSession);
        assertTrue(syncOpt.isDbVersionPresent());
    }

    @Test
    public void testIsDbVersionPresentNegative() {
        final Tlvs tlvs = createTlvs(null, true, false);
        doReturn(tlvs).when(pcepSession).getLocalTlvs();
        doReturn(tlvs).when(pcepSession).getRemoteTlvs();
        final SyncOptimization syncOpt = new SyncOptimization(pcepSession);
        assertFalse(syncOpt.isDbVersionPresent());
    }

    private static Tlvs createTlvs(final Long lspDbVersion, final boolean includeDbVresion,
            final boolean includeDeltaSync) {
        return new TlvsBuilder()
            .addAugmentation(new Tlvs1Builder()
                .setStateful(new StatefulBuilder()
                    .addAugmentation(new Stateful1Builder()
                        .setIncludeDbVersion(includeDbVresion)
                        .setDeltaLspSyncCapability(includeDeltaSync)
                        .build())
                    .build())
                .build())
            .addAugmentation(new Tlvs3Builder()
                .setLspDbVersion(new LspDbVersionBuilder()
                    .setLspDbVersionValue(lspDbVersion != null ? Uint64.valueOf(lspDbVersion) : null)
                    .build())
                .build())
            .build();
    }
}
