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
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.bgpcep.pcep.topology.provider.PCEPStatefulPeerProposal.LspDbVersionListener;
import org.opendaylight.bgpcep.pcep.topology.provider.PCEPStatefulPeerProposal.SpeakerIdListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.config.rev250930.pcep.node.config.SessionConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.speaker.entity.id.tlv.SpeakerEntityIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250930.stateful.capability.tlv.StatefulCapabilityBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.Uint64;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class PCEPStatefulPeerProposalTest {
    private static final DataObjectIdentifier.WithKey<Topology, TopologyKey> TOPOLOGY_IID =
        DataObjectIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId("topology")))
            .build();
    private static final LspDbVersion LSP_DB_VERSION = new LspDbVersionBuilder()
        .setLspDbVersionValue(Uint64.ONE)
        .build();
    private static final byte[] SPEAKER_ID = {0x01, 0x02, 0x03, 0x04};
    private static final InetSocketAddress ADDRESS = new InetSocketAddress(4321);

    @Mock
    private DataBroker dataBroker;
    @Mock
    private Registration listenerReg;
    @Mock
    private FluentFuture<Optional<LspDbVersion>> listenableFutureMock;
    @Captor
    private ArgumentCaptor<DataTreeChangeListener<?>> captor;

    private TlvsBuilder tlvsBuilder;

    @Before
    public void setUp() {
        tlvsBuilder = new TlvsBuilder().setStatefulCapability(new StatefulCapabilityBuilder().build());
        doReturn(listenerReg).when(dataBroker).registerTreeChangeListener(any(), any(), captor.capture());
        doNothing().when(listenerReg).close();
    }

    @Test
    public void testSetPeerProposalSuccess() throws Exception {
        updateBuilder(() -> {
            final var listeners = captor.getAllValues();
            assertEquals(2, listeners.size());

            // not entirely accurate, but works well enough
            final var modPath = TOPOLOGY_IID.toBuilder()
                .child(Node.class, new NodeKey(ServerSessionManager.createNodeId(ADDRESS.getAddress())))
                .build();

            final var dbverRoot = mock(DataObjectModification.class);
            doReturn(LSP_DB_VERSION).when(dbverRoot).dataAfter();
            final var dbverMod = mock(DataTreeModification.class);
            doReturn(modPath).when(dbverMod).path();
            doReturn(dbverRoot).when(dbverMod).getRootNode();

            for (var listener : listeners) {
                if (listener instanceof LspDbVersionListener) {
                    listener.onDataTreeChanged(List.of(dbverMod));
                }
            }

            // Mock lspdb
        });
        final var expected = new TlvsBuilder()
            .setLspDbVersion(LSP_DB_VERSION)
            .setStatefulCapability(new StatefulCapabilityBuilder().build())
            .setSpeakerEntityId(new SpeakerEntityIdBuilder().build())
            .build();
        assertEquals(expected, tlvsBuilder.build());
    }

    @Test
    public void testSetPeerProposalWithEntityIdSuccess() throws Exception {
        updateBuilder(() -> {
            final var listeners = captor.getAllValues();
            assertEquals(2, listeners.size());

            // not entirely accurate, but works well enough
            final var modPath = TOPOLOGY_IID.toBuilder()
                .child(Node.class, new NodeKey(ServerSessionManager.createNodeId(ADDRESS.getAddress())))
                .build();

            final var dbverRoot = mock(DataObjectModification.class);
            doReturn(LSP_DB_VERSION).when(dbverRoot).dataAfter();
            final var dbverMod = mock(DataTreeModification.class);
            doReturn(modPath).when(dbverMod).path();
            doReturn(dbverRoot).when(dbverMod).getRootNode();

            final var speakerRoot = mock(DataObjectModification.class);
            doReturn(new SessionConfigBuilder().setSpeakerEntityIdValue(SPEAKER_ID).build()).when(speakerRoot)
                .dataAfter();
            final var speakerMod = mock(DataTreeModification.class);
            doReturn(modPath).when(speakerMod).path();
            doReturn(speakerRoot).when(speakerMod).getRootNode();

            for (var listener : listeners) {
                if (listener instanceof SpeakerIdListener) {
                    listener.onDataTreeChanged(List.of(speakerMod));
                } else if (listener instanceof LspDbVersionListener) {
                    listener.onDataTreeChanged(List.of(dbverMod));
                }
            }
        });
        assertNotNull(tlvsBuilder.getLspDbVersion());
        assertEquals(LSP_DB_VERSION, tlvsBuilder.getLspDbVersion());
        assertEquals(new SpeakerEntityIdBuilder().setSpeakerEntityIdValue(SPEAKER_ID).build(),
                tlvsBuilder.getSpeakerEntityId());
    }

    @Test
    public void testSetPeerProposalAbsent() throws Exception {
        updateBuilder();
        assertNull(tlvsBuilder.getLspDbVersion());
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
