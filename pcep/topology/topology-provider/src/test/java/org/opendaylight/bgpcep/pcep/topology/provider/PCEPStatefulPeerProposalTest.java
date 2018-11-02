/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.topology.provider;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import com.google.common.util.concurrent.FluentFuture;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PCEPStatefulPeerProposalTest {

    private static final InstanceIdentifier<Topology> TOPOLOGY_IID = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId("topology")));
    private static final NodeId NODE_ID = new NodeId("node");
    private static final LspDbVersion LSP_DB_VERSION = new LspDbVersionBuilder().setLspDbVersionValue(
            BigInteger.ONE).build();
    private static final byte[] SPEAKER_ID = {0x01, 0x02, 0x03, 0x04};

    @Mock
    private DataBroker dataBroker;
    @Mock
    private FluentFuture<Optional<LspDbVersion>> listenableFutureMock;
    @Mock
    private ReadTransaction rt;
    private TlvsBuilder tlvsBuilder;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.tlvsBuilder = new TlvsBuilder().addAugmentation(
                Tlvs1.class,
                new Tlvs1Builder().setStateful(
                        new StatefulBuilder().addAugmentation(Stateful1.class, new Stateful1Builder().build()).build())
                        .build());
        doReturn(this.rt).when(this.dataBroker).newReadOnlyTransaction();
        doNothing().when(this.rt).close();
        doReturn(this.listenableFutureMock).when(this.rt)
                .read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        doReturn(true).when(this.listenableFutureMock).isDone();
        doAnswer(invocation -> {
            final Runnable runnable = (Runnable) invocation.getArguments()[0];
            runnable.run();
            return null;
        }).when(this.listenableFutureMock).addListener(any(Runnable.class), any(Executor.class));
    }

    @Test
    public void testSetPeerProposalSuccess() throws InterruptedException, ExecutionException {
        doReturn(Optional.of(LSP_DB_VERSION)).when(this.listenableFutureMock).get();
        final PCEPStatefulPeerProposal peerProposal = PCEPStatefulPeerProposal
                .createStatefulPeerProposal(this.dataBroker, TOPOLOGY_IID);
        peerProposal.setPeerProposal(NODE_ID, this.tlvsBuilder, null);
        assertEquals(LSP_DB_VERSION, this.tlvsBuilder.augmentation(Tlvs3.class).getLspDbVersion());
    }

    @Test
    public void testSetPeerProposalWithEntityIdSuccess() throws InterruptedException, ExecutionException {
        doReturn(Optional.of(LSP_DB_VERSION)).when(this.listenableFutureMock).get();
        final PCEPStatefulPeerProposal peerProposal = PCEPStatefulPeerProposal
                .createStatefulPeerProposal(this.dataBroker, TOPOLOGY_IID);
        peerProposal.setPeerProposal(NODE_ID, this.tlvsBuilder, SPEAKER_ID);
        final Tlvs3 aug = this.tlvsBuilder.augmentation(Tlvs3.class);
        assertEquals(LSP_DB_VERSION, aug.getLspDbVersion());
        assertArrayEquals(SPEAKER_ID, aug.getSpeakerEntityId().getSpeakerEntityIdValue());
    }

    @Test
    public void testSetPeerProposalAbsent() throws InterruptedException, ExecutionException {
        doReturn(Optional.empty()).when(this.listenableFutureMock).get();
        final PCEPStatefulPeerProposal peerProposal = PCEPStatefulPeerProposal
                .createStatefulPeerProposal(this.dataBroker, TOPOLOGY_IID);
        peerProposal.setPeerProposal(NODE_ID, this.tlvsBuilder, null);
        assertNull(this.tlvsBuilder.augmentation(Tlvs3.class));
    }

    @Test
    public void testSetPeerProposalFailure() throws InterruptedException, ExecutionException {
        doThrow(new InterruptedException()).when(this.listenableFutureMock).get();
        final PCEPStatefulPeerProposal peerProposal = PCEPStatefulPeerProposal
                .createStatefulPeerProposal(this.dataBroker, TOPOLOGY_IID);
        peerProposal.setPeerProposal(NODE_ID, this.tlvsBuilder, null);
        assertNull(this.tlvsBuilder.augmentation(Tlvs3.class));
    }

}
