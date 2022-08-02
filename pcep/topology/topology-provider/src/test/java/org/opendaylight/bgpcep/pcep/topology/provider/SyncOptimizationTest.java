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

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.TlvsBuilder;
import org.opendaylight.yangtools.yang.common.Uint64;

public class SyncOptimizationTest {
    @Test
    public void testDoesLspDbMatchPositive() {
        final var tlvs = createTlvs(1L, false, false);
        assertTrue(new SyncOptimization(tlvs, tlvs).doesLspDbMatch());
    }

    @Test
    public void testDoesLspDbMatchNegative() {
        assertFalse(new SyncOptimization(createTlvs(1L, false, false), createTlvs(2L, false, false)).doesLspDbMatch());
    }

    @Test
    public void testIsSyncAvoidanceEnabledPositive() {
        final var tlvs = createTlvs(1L, true, false);
        assertTrue(new SyncOptimization(tlvs, tlvs).isSyncAvoidanceEnabled());
    }

    @Test
    public void testIsSyncAvoidanceEnabledNegative() {
        assertFalse(new SyncOptimization(createTlvs(1L, true, false), createTlvs(2L, false, false))
            .isSyncAvoidanceEnabled());
    }

    @Test
    public void testIsDeltaSyncEnabledPositive() {
        final var tlvs = createTlvs(1L, true, true);
        assertTrue(new SyncOptimization(tlvs, tlvs).isDeltaSyncEnabled());
    }

    @Test
    public void testIsDeltaSyncEnabledNegative() {
        assertFalse(new SyncOptimization(createTlvs(1L, true, true), createTlvs(2L, false, false))
            .isDeltaSyncEnabled());
    }

    @Test
    public void testIsDbVersionPresentPositive() {
        assertTrue(new SyncOptimization(createTlvs(null, false, false), createTlvs(2L, false, false))
            .isDbVersionPresent());
    }

    @Test
    public void testIsDbVersionPresentNegative() {
        final var tlvs = createTlvs(null, true, false);
        assertFalse(new SyncOptimization(tlvs, tlvs).isDbVersionPresent());
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
