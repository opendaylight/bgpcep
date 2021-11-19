/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.FluentFuture;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.bgpcep.pcep.topology.provider.PCEPStatefulPeerProposal.LspDbVersionListener;
import org.opendaylight.bgpcep.pcep.topology.provider.PCEPStatefulPeerProposal.SpeakerIdListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.speaker.entity.id.tlv.SpeakerEntityIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.sync.optimizations.config.rev181109.PcepNodeSyncConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class PCEPStatefulPeerProposalTest {
    private static final InstanceIdentifier<Topology> TOPOLOGY_IID = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId("topology")));
    private static final LspDbVersion LSP_DB_VERSION = new LspDbVersionBuilder()
        .setLspDbVersionValue(Uint64.ONE)
        .build();
    private static final byte[] SPEAKER_ID = {0x01, 0x02, 0x03, 0x04};
    private static final InetSocketAddress ADDRESS = new InetSocketAddress(4321);

    @Mock
    private DataBroker dataBroker;
    @Mock
    private ListenerRegistration<?> listenerReg;
    @Mock
    private FluentFuture<Optional<LspDbVersion>> listenableFutureMock;

    private ArgumentCaptor<DataTreeChangeListener<?>> captor;
    private TlvsBuilder tlvsBuilder;

    @Before
    public void setUp() {
        tlvsBuilder = new TlvsBuilder().addAugmentation(new Tlvs1Builder()
            .setStateful(new StatefulBuilder().addAugmentation(new Stateful1Builder().build()).build())
            .build());
        captor = ArgumentCaptor.forClass(DataTreeChangeListener.class);
        doReturn(listenerReg).when(dataBroker).registerDataTreeChangeListener(any(), captor.capture());
        doNothing().when(listenerReg).close();
    }

    @Test
    public void testSetPeerProposalSuccess() throws Exception {
        updateBuilder(() -> {
            final var listeners = captor.getAllValues();
            assertEquals(2, listeners.size());

            // not entirely accurate, but works well enough
            final var modPath = TOPOLOGY_IID.child(Node.class,
                new NodeKey(ServerSessionManager.createNodeId(ADDRESS.getAddress())));

            final var dbverRoot = mock(DataObjectModification.class);
            doReturn(LSP_DB_VERSION).when(dbverRoot).getDataAfter();
            final var dbverMod = mock(DataTreeModification.class);
            doReturn(DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, modPath)).when(dbverMod).getRootPath();
            doReturn(dbverRoot).when(dbverMod).getRootNode();

            for (DataTreeChangeListener<?> listener : listeners) {
                if (listener instanceof LspDbVersionListener) {
                    listener.onDataTreeChanged(List.of(dbverMod));
                }
            }

            // Mock lspdb
        });
        assertEquals(new Tlvs3Builder().setLspDbVersion(LSP_DB_VERSION).build(), tlvsBuilder.augmentation(Tlvs3.class));
    }

    @Test
    public void testSetPeerProposalWithEntityIdSuccess() throws Exception {
        updateBuilder(() -> {
            final var listeners = captor.getAllValues();
            assertEquals(2, listeners.size());

            // not entirely accurate, but works well enough
            final var modPath = TOPOLOGY_IID.child(Node.class,
                new NodeKey(ServerSessionManager.createNodeId(ADDRESS.getAddress())));

            final var dbverRoot = mock(DataObjectModification.class);
            doReturn(LSP_DB_VERSION).when(dbverRoot).getDataAfter();
            final var dbverMod = mock(DataTreeModification.class);
            doReturn(DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, modPath)).when(dbverMod).getRootPath();
            doReturn(dbverRoot).when(dbverMod).getRootNode();

            final var speakerRoot = mock(DataObjectModification.class);
            doReturn(new PcepNodeSyncConfigBuilder().setSpeakerEntityIdValue(SPEAKER_ID).build()).when(speakerRoot)
                .getDataAfter();
            final var speakerMod = mock(DataTreeModification.class);
            doReturn(DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION, modPath)).when(speakerMod)
                .getRootPath();
            doReturn(speakerRoot).when(speakerMod).getRootNode();

            for (DataTreeChangeListener<?> listener : listeners) {
                if (listener instanceof SpeakerIdListener) {
                    listener.onDataTreeChanged(List.of(speakerMod));
                } else if (listener instanceof LspDbVersionListener) {
                    listener.onDataTreeChanged(List.of(dbverMod));
                }
            }
        });
        final Tlvs3 aug = tlvsBuilder.augmentation(Tlvs3.class);
        assertNotNull(aug);
        assertEquals(LSP_DB_VERSION, aug.getLspDbVersion());
        assertEquals(new SpeakerEntityIdBuilder().setSpeakerEntityIdValue(SPEAKER_ID).build(),
            aug.getSpeakerEntityId());
    }

    @Test
    public void testSetPeerProposalAbsent() throws Exception {
        updateBuilder();
        assertNull(tlvsBuilder.augmentation(Tlvs3.class));
    }

    private void updateBuilder() {
        updateBuilder(null);
    }

    private void updateBuilder(final Runnable customizer) {
        try (var proposal = new PCEPStatefulPeerProposal(dataBroker, TOPOLOGY_IID)) {
            if (customizer != null) {
                customizer.run();
            }
            proposal.setPeerSpecificProposal(ADDRESS, tlvsBuilder);
        }
    }
}
