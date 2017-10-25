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

import java.math.BigInteger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;

public class SyncOptimizationTest {

    @Mock
    private PCEPSession pcepSession;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDoesLspDbMatchPositive() {
        final Tlvs tlvs = createTlvs(1L, false, false);
        Mockito.doReturn(tlvs).when(this.pcepSession).localSessionCharacteristics();
        Mockito.doReturn(tlvs).when(this.pcepSession).getRemoteTlvs();
        final SyncOptimization syncOpt = new SyncOptimization(this.pcepSession);
        assertTrue(syncOpt.doesLspDbMatch());
    }

    @Test
    public void testDoesLspDbMatchNegative() {
        final Tlvs localTlvs = createTlvs(1L, false, false);
        final Tlvs remoteTlvs = createTlvs(2L, false, false);
        Mockito.doReturn(localTlvs).when(this.pcepSession).localSessionCharacteristics();
        Mockito.doReturn(remoteTlvs).when(this.pcepSession).getRemoteTlvs();
        final SyncOptimization syncOpt = new SyncOptimization(this.pcepSession);
        assertFalse(syncOpt.doesLspDbMatch());
    }

    @Test
    public void testIsSyncAvoidanceEnabledPositive() {
        final Tlvs tlvs = createTlvs(1L, true, false);
        Mockito.doReturn(tlvs).when(this.pcepSession).localSessionCharacteristics();
        Mockito.doReturn(tlvs).when(this.pcepSession).getRemoteTlvs();
        final SyncOptimization syncOpt = new SyncOptimization(this.pcepSession);
        assertTrue(syncOpt.isSyncAvoidanceEnabled());
    }

    @Test
    public void testIsSyncAvoidanceEnabledNegative() {
        final Tlvs localTlvs = createTlvs(1L, true, false);
        final Tlvs remoteTlvs = createTlvs(2L, false, false);
        Mockito.doReturn(localTlvs).when(this.pcepSession).localSessionCharacteristics();
        Mockito.doReturn(remoteTlvs).when(this.pcepSession).getRemoteTlvs();
        final SyncOptimization syncOpt = new SyncOptimization(this.pcepSession);
        assertFalse(syncOpt.isSyncAvoidanceEnabled());
    }

    @Test
    public void testIsDeltaSyncEnabledPositive() {
        final Tlvs tlvs = createTlvs(1L, true, true);
        Mockito.doReturn(tlvs).when(this.pcepSession).localSessionCharacteristics();
        Mockito.doReturn(tlvs).when(this.pcepSession).getRemoteTlvs();
        final SyncOptimization syncOpt = new SyncOptimization(this.pcepSession);
        assertTrue(syncOpt.isDeltaSyncEnabled());
    }

    @Test
    public void testIsDeltaSyncEnabledNegative() {
        final Tlvs localTlvs = createTlvs(1L, true, true);
        final Tlvs remoteTlvs = createTlvs(2L, false, false);
        Mockito.doReturn(localTlvs).when(this.pcepSession).localSessionCharacteristics();
        Mockito.doReturn(remoteTlvs).when(this.pcepSession).getRemoteTlvs();
        final SyncOptimization syncOpt = new SyncOptimization(this.pcepSession);
        assertFalse(syncOpt.isDeltaSyncEnabled());
    }

    @Test
    public void testIsDbVersionPresentPositive() {
        final Tlvs localTlvs = createTlvs(null, false, false);
        final Tlvs remoteTlvs = createTlvs(2L, false, false);
        Mockito.doReturn(localTlvs).when(this.pcepSession).localSessionCharacteristics();
        Mockito.doReturn(remoteTlvs).when(this.pcepSession).getRemoteTlvs();
        final SyncOptimization syncOpt = new SyncOptimization(this.pcepSession);
        assertTrue(syncOpt.isDbVersionPresent());
    }

    @Test
    public void testIsDbVersionPresentNegative() {
        final Tlvs tlvs = createTlvs(null, true, false);
        Mockito.doReturn(tlvs).when(this.pcepSession).localSessionCharacteristics();
        Mockito.doReturn(tlvs).when(this.pcepSession).getRemoteTlvs();
        final SyncOptimization syncOpt = new SyncOptimization(this.pcepSession);
        assertFalse(syncOpt.isDbVersionPresent());
    }

    private static Tlvs createTlvs(final Long lspDbVersion, final boolean includeDbVresion, final boolean includeDeltaSync) {
        return new TlvsBuilder()
            .addAugmentation(Tlvs1.class, new Tlvs1Builder().setStateful(
                new StatefulBuilder().addAugmentation(Stateful1.class,
                        new Stateful1Builder()
                            .setIncludeDbVersion(includeDbVresion).setDeltaLspSyncCapability(includeDeltaSync).build()).build()).build())
            .addAugmentation(Tlvs3.class, new Tlvs3Builder().setLspDbVersion(
                new LspDbVersionBuilder().setLspDbVersionValue(lspDbVersion != null ? BigInteger.valueOf(lspDbVersion) : null).build()).build()).build();
    }
}
