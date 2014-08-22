/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.topology.provider;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.rev140113.NetworkTopologyRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated._00.rev140113.Pcinitiate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Arguments1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Arguments1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Arguments2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Arguments2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.add.lsp.args.ArgumentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.ReportedLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.reported.lsp.Path;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;

public class Stateful02TopologySessionListenerTest extends AbstractPCEPSessionTest<Stateful02TopologySessionListenerFactory> {

    private Stateful02TopologySessionListener listener;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        listener = (Stateful02TopologySessionListener) this.manager.getSessionListener();
    }

    @Test
    public void testStateful02TopologySessionListener() throws Exception {
        this.listener.onSessionUp(this.session);

        // add-lsp
        final ArgumentsBuilder argsBuilder = new ArgumentsBuilder();
        final Ipv4CaseBuilder ipv4Builder = new Ipv4CaseBuilder();
        ipv4Builder.setIpv4(new Ipv4Builder().setSourceIpv4Address(new Ipv4Address(TEST_ADDRESS)).setDestinationIpv4Address(new Ipv4Address(TEST_ADDRESS)).build());
        argsBuilder.setEndpointsObj(new EndpointsObjBuilder().setAddressFamily(ipv4Builder.build()).build());
        final AddLspInput input = new AddLspInputBuilder().setName(TEST_LSP_NAME).setArguments(argsBuilder.build()).setNetworkTopologyRef(new NetworkTopologyRef(TOPO_IID)).setNode(NODE_ID).build();
        this.topologyRpcs.addLsp(input);
        assertEquals(1, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(0) instanceof Pcinitiate);
        final Pcrpt pcRpt = createPcrpt02(0L, false, null, Optional.of(TEST_LSP_NAME));

        final Optional<Topology> topoOptional = getTopology();
        assertTrue(topoOptional.isPresent());
        Topology topology = topoOptional.get();
        assertEquals(1, topology.getNode().size());
        final Node1 node = topology.getNode().get(0).getAugmentation(Node1.class);
        assertNotNull(node);
        PathComputationClient pcc = node.getPathComputationClient();
        assertEquals(TEST_ADDRESS, pcc.getIpAddress().getIpv4Address().getValue());
        // reported lsp so far empty, has not received response (PcRpt) yet
        assertNull(pcc.getReportedLsp());
        this.listener.onMessage(this.session, pcRpt);
        // check created lsp
        topology = getTopology().get();
        pcc = topology.getNode().get(0).getAugmentation(Node1.class).getPathComputationClient();
        assertEquals(1, pcc.getReportedLsp().size());
        ReportedLsp reportedLsp = pcc.getReportedLsp().get(0);
        assertEquals(TEST_LSP_NAME, reportedLsp.getName());
        assertEquals(1, reportedLsp.getPath().size());

        // update-lsp
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.update.lsp.args.ArgumentsBuilder updArgsBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.update.lsp.args.ArgumentsBuilder();
        updArgsBuilder.setEro(createEroWithIpPrefixes(Lists.newArrayList(ERO_IP_PREFIX, DST_IP_PREFIX)));
        updArgsBuilder.addAugmentation(Arguments2.class, new Arguments2Builder().setOperational(true).build());
        final UpdateLspInput update = new UpdateLspInputBuilder().setArguments(updArgsBuilder.build()).setName(TEST_LSP_NAME).setNetworkTopologyRef(new NetworkTopologyRef(TOPO_IID)).setNode(NODE_ID).build();
        this.topologyRpcs.updateLsp(update);
        assertEquals(2, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(1) instanceof Pcupd);
        final Pcupd updateMsg = (Pcupd) this.receivedMsgs.get(1);
        final Updates upd = updateMsg.getPcupdMessage().getUpdates().get(0);
        final Pcrpt pcRpt2 = createPcrpt02(upd.getLsp().getPlspId().getValue(), false, upd.getPath().getEro(), Optional.<String>absent());
        this.listener.onMessage(this.session, pcRpt2);
        //check updated lsp
        topology = getTopology().get();
        pcc = topology.getNode().get(0).getAugmentation(Node1.class).getPathComputationClient();
        assertEquals(1, pcc.getReportedLsp().size());
        reportedLsp = pcc.getReportedLsp().get(0);
        assertEquals(TEST_LSP_NAME, reportedLsp.getName());
        assertEquals(1, reportedLsp.getPath().size());
        Path path = reportedLsp.getPath().get(0);
        assertEquals(2, path.getEro().getSubobject().size());
        assertEquals(DST_IP_PREFIX, getLastEroIpPrefix(path.getEro()));

        // ensure-operational
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.ensure.lsp.operational.args.ArgumentsBuilder ensureArgs = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.ensure.lsp.operational.args.ArgumentsBuilder();
        ensureArgs.addAugmentation(Arguments1.class, new Arguments1Builder().setOperational(true).build());
        final EnsureLspOperationalInput ensure = new EnsureLspOperationalInputBuilder().setArguments(ensureArgs.build()).setName(TEST_LSP_NAME).setNetworkTopologyRef(new NetworkTopologyRef(TOPO_IID)).setNode(NODE_ID).build();
        final OperationResult result = this.topologyRpcs.ensureLspOperational(ensure).get().getResult();
        //check result
        assertNull(result.getFailure());

        // remove-lsp
        final RemoveLspInput remove = new RemoveLspInputBuilder().setName(TEST_LSP_NAME).setNetworkTopologyRef(new NetworkTopologyRef(TOPO_IID)).setNode(NODE_ID).build();
        this.topologyRpcs.removeLsp(remove);
        assertEquals(3, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(2) instanceof Pcupd);
        final Pcupd pcUpd2 = (Pcupd) this.receivedMsgs.get(2);
        final Updates upd2 = pcUpd2.getPcupdMessage().getUpdates().get(0);
        final Pcrpt pcRpt3 = createPcrpt02(upd2.getLsp().getPlspId().getValue(), true, null, Optional.<String>absent());
        this.listener.onMessage(this.session, pcRpt3);
        // check if lsp was removed
        topology = getTopology().get();
        pcc = topology.getNode().get(0).getAugmentation(Node1.class).getPathComputationClient();
        assertEquals(0, pcc.getReportedLsp().size());
    }

    private Pcrpt createPcrpt02(final long plspId, final boolean remove, final Ero ero, final Optional<String> spn) {
        final ReportsBuilder rptBuilder = new ReportsBuilder();
        final TlvsBuilder tlvsBuilder = new TlvsBuilder();
        if (spn.isPresent()) {
            tlvsBuilder.setSymbolicPathName(new SymbolicPathNameBuilder().setPathName(new SymbolicPathName(Unpooled.wrappedBuffer(CharsetUtil.UTF_8.encode(spn.get())).array())).build());
        }
        rptBuilder.setLsp(new LspBuilder().setDelegate(true).setOperational(true).setPlspId(new PlspId(plspId)).setRemove(remove).setSync(true).setTlvs(tlvsBuilder.build()).build());
        rptBuilder.setPath(new PathBuilder().setEro(ero).build());
        return new PcrptBuilder().setPcrptMessage(new PcrptMessageBuilder().setReports(Lists.newArrayList(rptBuilder.build())).build()).build();
    }

}
