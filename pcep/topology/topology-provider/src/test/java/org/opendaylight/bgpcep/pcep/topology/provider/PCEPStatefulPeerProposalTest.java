/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.topology.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Tlvs3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.lsp.db.version.tlv.LspDbVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.StatefulBuilder;
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

    @Mock
    private DataBroker dataBroker;
    @Mock
    private CheckedFuture<Optional<LspDbVersion>, ReadFailedException> listenableFutureMock;
    @Mock
    private ReadOnlyTransaction rTxMock;
    private TlvsBuilder tlvsBuilder;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws InterruptedException, ExecutionException {
        MockitoAnnotations.initMocks(this);
        this.tlvsBuilder = new TlvsBuilder().addAugmentation(
                Tlvs1.class,
                new Tlvs1Builder().setStateful(
                        new StatefulBuilder().addAugmentation(Stateful1.class, new Stateful1Builder().build()).build())
                        .build());
        doReturn(this.rTxMock).when(this.dataBroker).newReadOnlyTransaction();
        doNothing().when(this.rTxMock).close();
        doReturn(this.listenableFutureMock).when(this.rTxMock)
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
        final PCEPStatefulPeerProposal peerProposal = PCEPStatefulPeerProposal.createStatefulPeerProposal(this.dataBroker,
                TOPOLOGY_IID);
        peerProposal.setPeerProposal(NODE_ID, this.tlvsBuilder);
        assertEquals(LSP_DB_VERSION, this.tlvsBuilder.getAugmentation(Tlvs3.class).getLspDbVersion());
    }

    @Test
    public void testSetPeerProposalAbsent() throws InterruptedException, ExecutionException {
        doReturn(Optional.absent()).when(this.listenableFutureMock).get();
        final PCEPStatefulPeerProposal peerProposal = PCEPStatefulPeerProposal.createStatefulPeerProposal(this.dataBroker,
                TOPOLOGY_IID);
        peerProposal.setPeerProposal(NODE_ID, this.tlvsBuilder);
        assertNull(this.tlvsBuilder.getAugmentation(Tlvs3.class));
    }

    @Test
    public void testSetPeerProposalFailure() throws InterruptedException, ExecutionException {
        doThrow(new RuntimeException()).when(this.listenableFutureMock).get();
        final PCEPStatefulPeerProposal peerProposal = PCEPStatefulPeerProposal.createStatefulPeerProposal(this.dataBroker,
                TOPOLOGY_IID);
        peerProposal.setPeerProposal(NODE_ID, this.tlvsBuilder);
        assertNull(this.tlvsBuilder.getAugmentation(Tlvs3.class));
    }

}
