/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.bgpcep.pcep.topology.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.protocol.pcep.pcc.mock.MsgBuilderUtil.createLspTlvs;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.yang.pcep.topology.provider.SessionState;
import org.opendaylight.protocol.pcep.PCEPCloseTermination;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.TerminationReason;
import org.opendaylight.protocol.pcep.pcc.mock.MsgBuilderUtil;
import org.opendaylight.protocol.pcep.spi.AbstractMessageParser;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.rev140113.NetworkTopologyRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Pcinitiate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.Stateful1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Arguments1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Arguments1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Arguments2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Arguments2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Arguments3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Arguments3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Pcupd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.Reports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.FailureType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.Node1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.add.lsp.args.ArgumentsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.PathComputationClient;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.ReportedLsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.pcep.client.attributes.path.computation.client.reported.lsp.Path;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class Stateful07TopologySessionListenerTest extends AbstractPCEPSessionTest<Stateful07TopologySessionListenerFactory> {

    private static final String TUNNEL_NAME = "pcc_" + TEST_ADDRESS + "_tunnel_0";

    private Stateful07TopologySessionListener listener;

    private PCEPSession session;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.listener = (Stateful07TopologySessionListener) getSessionListener();
        this.session = getPCEPSession(getLocalPref(), getRemotePref());
    }

    @Test
    public void testStateful07TopologySessionListener() throws Exception {
        this.listener.onSessionUp(this.session);

        assertEquals(TEST_ADDRESS, this.listener.getPeerId());
        final SessionState state = this.listener.getSessionState();
        assertNotNull(state);
        assertEquals(DEAD_TIMER, state.getLocalPref().getDeadtimer().shortValue());
        assertEquals(KEEP_ALIVE, state.getLocalPref().getKeepalive().shortValue());
        assertEquals(0, state.getLocalPref().getSessionId().intValue());
        assertEquals(TEST_ADDRESS, state.getLocalPref().getIpAddress());
        assertEquals(DEAD_TIMER, state.getPeerPref().getDeadtimer().shortValue());
        assertEquals(KEEP_ALIVE, state.getPeerPref().getKeepalive().shortValue());
        assertEquals(0, state.getPeerPref().getSessionId().intValue());
        assertEquals(TEST_ADDRESS, state.getPeerPref().getIpAddress());

        // add-lsp
        this.topologyRpcs.addLsp(createAddLspInput());
        assertEquals(1, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(0) instanceof Pcinitiate);
        final Pcinitiate pcinitiate = (Pcinitiate) this.receivedMsgs.get(0);
        final Requests req = pcinitiate.getPcinitiateMessage().getRequests().get(0);
        final long srpId = req.getSrp().getOperationId().getValue();
        final Tlvs tlvs = createLspTlvs(req.getLsp().getPlspId().getValue(), true,
                TEST_ADDRESS, TEST_ADDRESS, TEST_ADDRESS, Optional.<byte[]>absent());
        final Pcrpt pcRpt = MsgBuilderUtil.createPcRtpMessage(new LspBuilder(req.getLsp()).setTlvs(tlvs).setPlspId(new PlspId(1L)).setSync(false).setRemove(false).setOperational(OperationalStatus.Active).build(), Optional.of(MsgBuilderUtil.createSrp(srpId)), MsgBuilderUtil.createPath(req.getEro().getSubobject()));
        final Pcrpt esm = MsgBuilderUtil.createPcRtpMessage(new LspBuilder().setSync(false).build(), Optional.of(MsgBuilderUtil.createSrp(0L)), null);
        this.listener.onMessage(this.session, esm);

        final Optional<Topology> topoOptional = getTopology();
        assertTrue(topoOptional.isPresent());
        Topology topology = topoOptional.get();
        assertEquals(1, topology.getNode().size());
        final Node1 node = topology.getNode().get(0).getAugmentation(Node1.class);
        assertNotNull(node);
        PathComputationClient pcc = node.getPathComputationClient();
        assertEquals(TEST_ADDRESS, pcc.getIpAddress().getIpv4Address().getValue());
        // reported lsp so far empty, has not received response (PcRpt) yet
        assertTrue(pcc.getReportedLsp().isEmpty());
        this.listener.onMessage(this.session, pcRpt);
        // check created lsp
        topology = getTopology().get();
        pcc = topology.getNode().get(0).getAugmentation(Node1.class).getPathComputationClient();
        assertEquals(1, pcc.getReportedLsp().size());
        ReportedLsp reportedLsp = pcc.getReportedLsp().get(0);
        assertEquals(TUNNEL_NAME, reportedLsp.getName());
        assertEquals(1, reportedLsp.getPath().size());
        Path path = reportedLsp.getPath().get(0);
        assertEquals(1, path.getEro().getSubobject().size());
        assertEquals(ERO_IP_PREFIX, getLastEroIpPrefix(path.getEro()));
        // check stats
        assertEquals(1, this.listener.getDelegatedLspsCount().intValue());
        assertTrue(this.listener.getSynchronized());
        assertTrue(this.listener.getStatefulMessages().getLastReceivedRptMsgTimestamp() > 0);
        assertEquals(2, this.listener.getStatefulMessages().getReceivedRptMsgCount().intValue());
        assertEquals(1, this.listener.getStatefulMessages().getSentInitMsgCount().intValue());
        assertEquals(0, this.listener.getStatefulMessages().getSentUpdMsgCount().intValue());
        assertNotNull(this.listener.getSessionState());

        // update-lsp
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.update.lsp.args.ArgumentsBuilder updArgsBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.update.lsp.args.ArgumentsBuilder();
        updArgsBuilder.setEro(createEroWithIpPrefixes(Lists.newArrayList(ERO_IP_PREFIX, DST_IP_PREFIX)));
        updArgsBuilder.addAugmentation(Arguments3.class, new Arguments3Builder().setLsp(new LspBuilder().setDelegate(true).setAdministrative(true).build()).build());
        final UpdateLspInput update = new UpdateLspInputBuilder().setArguments(updArgsBuilder.build()).setName(TUNNEL_NAME).setNetworkTopologyRef(new NetworkTopologyRef(TOPO_IID)).setNode(NODE_ID).build();
        this.topologyRpcs.updateLsp(update);
        assertEquals(2, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(1) instanceof Pcupd);
        final Pcupd updateMsg = (Pcupd) this.receivedMsgs.get(1);
        final Updates upd = updateMsg.getPcupdMessage().getUpdates().get(0);
        final long srpId2 = upd.getSrp().getOperationId().getValue();
        final Tlvs tlvs2 = createLspTlvs(upd.getLsp().getPlspId().getValue(), false,
                NEW_DESTINATION_ADDRESS, TEST_ADDRESS, TEST_ADDRESS, Optional.<byte[]>absent());
        final Pcrpt pcRpt2 = MsgBuilderUtil.createPcRtpMessage(new LspBuilder(upd.getLsp()).setTlvs(tlvs2).setSync(true).setRemove(false).setOperational(OperationalStatus.Active).build(), Optional.of(MsgBuilderUtil.createSrp(srpId2)), MsgBuilderUtil.createPath(upd.getPath().getEro().getSubobject()));
        this.listener.onMessage(this.session, pcRpt2);
        //check updated lsp
        topology = getTopology().get();
        pcc = topology.getNode().get(0).getAugmentation(Node1.class).getPathComputationClient();
        assertEquals(1, pcc.getReportedLsp().size());
        reportedLsp = pcc.getReportedLsp().get(0);
        assertEquals(TUNNEL_NAME, reportedLsp.getName());
        assertEquals(1, reportedLsp.getPath().size());
        path = reportedLsp.getPath().get(0);
        assertEquals(2, path.getEro().getSubobject().size());
        assertEquals(DST_IP_PREFIX, getLastEroIpPrefix(path.getEro()));
        // check stats
        assertEquals(1, this.listener.getDelegatedLspsCount().intValue());
        assertTrue(this.listener.getSynchronized());
        assertTrue(this.listener.getStatefulMessages().getLastReceivedRptMsgTimestamp() > 0);
        assertEquals(3, this.listener.getStatefulMessages().getReceivedRptMsgCount().intValue());
        assertEquals(1, this.listener.getStatefulMessages().getSentInitMsgCount().intValue());
        assertEquals(1, this.listener.getStatefulMessages().getSentUpdMsgCount().intValue());
        assertTrue(this.listener.getReplyTime().getAverageTime() > 0);
        assertTrue(this.listener.getReplyTime().getMaxTime() > 0);
        assertFalse(this.listener.getPeerCapabilities().getActive());
        assertTrue(this.listener.getPeerCapabilities().getInstantiation());
        assertTrue(this.listener.getPeerCapabilities().getStateful());

        // ensure-operational
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.ensure.lsp.operational.args.ArgumentsBuilder ensureArgs = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.ensure.lsp.operational.args.ArgumentsBuilder();
        ensureArgs.addAugmentation(Arguments1.class, new Arguments1Builder().setOperational(OperationalStatus.Active).build());
        final EnsureLspOperationalInput ensure = new EnsureLspOperationalInputBuilder().setArguments(ensureArgs.build()).setName(TUNNEL_NAME).setNetworkTopologyRef(new NetworkTopologyRef(TOPO_IID)).setNode(NODE_ID).build();
        final OperationResult result = this.topologyRpcs.ensureLspOperational(ensure).get().getResult();
        //check result
        assertNull(result.getFailure());

        // remove-lsp
        final RemoveLspInput remove = new RemoveLspInputBuilder().setName(TUNNEL_NAME).setNetworkTopologyRef(new NetworkTopologyRef(TOPO_IID)).setNode(NODE_ID).build();
        this.topologyRpcs.removeLsp(remove);
        assertEquals(3, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(2) instanceof Pcinitiate);
        final Pcinitiate pcinitiate2 = (Pcinitiate) this.receivedMsgs.get(2);
        final Requests req2 = pcinitiate2.getPcinitiateMessage().getRequests().get(0);
        final long srpId3 = req2.getSrp().getOperationId().getValue();
        final Tlvs tlvs3 = createLspTlvs(req2.getLsp().getPlspId().getValue(), false,
                TEST_ADDRESS, TEST_ADDRESS, TEST_ADDRESS, Optional.<byte[]>absent());
        final Pcrpt pcRpt3 = MsgBuilderUtil.createPcRtpMessage(new LspBuilder(req2.getLsp()).setTlvs(tlvs3).setRemove(true).setSync(true).setOperational(OperationalStatus.Down).build(), Optional.of(MsgBuilderUtil.createSrp(srpId3)), MsgBuilderUtil.createPath(Collections.<Subobject>emptyList()));
        this.listener.onMessage(this.session, pcRpt3);
        // check if lsp was removed
        topology = getTopology().get();
        pcc = topology.getNode().get(0).getAugmentation(Node1.class).getPathComputationClient();
        assertEquals(0, pcc.getReportedLsp().size());
        // check stats
        assertEquals(0, this.listener.getDelegatedLspsCount().intValue());
        assertTrue(this.listener.getSynchronized());
        assertTrue(this.listener.getStatefulMessages().getLastReceivedRptMsgTimestamp() > 0);
        assertEquals(4, this.listener.getStatefulMessages().getReceivedRptMsgCount().intValue());
        assertEquals(2, this.listener.getStatefulMessages().getSentInitMsgCount().intValue());
        assertEquals(1, this.listener.getStatefulMessages().getSentUpdMsgCount().intValue());
        this.listener.resetStats();
        assertEquals(0, this.listener.getStatefulMessages().getLastReceivedRptMsgTimestamp().longValue());
        assertEquals(0, this.listener.getStatefulMessages().getReceivedRptMsgCount().intValue());
        assertEquals(0, this.listener.getStatefulMessages().getSentInitMsgCount().intValue());
        assertEquals(0, this.listener.getStatefulMessages().getSentUpdMsgCount().intValue());
        assertEquals(0, this.listener.getReplyTime().getAverageTime().longValue());
        assertEquals(0, this.listener.getReplyTime().getMaxTime().longValue());
        assertEquals(0, this.listener.getReplyTime().getMinTime().longValue());
    }

    @Test
    public void testOnUnhandledErrorMessage() {
        final Message errorMsg = AbstractMessageParser.createErrorMsg(PCEPErrors.NON_ZERO_PLSPID, Optional.<Rp>absent());
        this.listener.onSessionUp(this.session);
        assertTrue(this.listener.onMessage(Optional.<AbstractTopologySessionListener.MessageContext>absent().orNull(), errorMsg));
    }

    @Test
    public void testOnErrorMessage() throws InterruptedException, ExecutionException {
        final Message errorMsg = MsgBuilderUtil.createErrorMsg(PCEPErrors.NON_ZERO_PLSPID, 1L);
        this.listener.onSessionUp(this.session);
        final Future<RpcResult<AddLspOutput>> futureOutput = this.topologyRpcs.addLsp(createAddLspInput());
        this.listener.onMessage(this.session, errorMsg);

        final AddLspOutput output = futureOutput.get().getResult();
        assertEquals(FailureType.Failed ,output.getFailure());
        assertEquals(1, output.getError().size());
        final ErrorObject err = output.getError().get(0).getErrorObject();
        assertEquals(PCEPErrors.NON_ZERO_PLSPID.getErrorType(), err.getType().shortValue());
        assertEquals(PCEPErrors.NON_ZERO_PLSPID.getErrorValue(), err.getValue().shortValue());
    }

    @Test
    public void testOnSessionDown() throws InterruptedException, ExecutionException {
        this.listener.onSessionUp(this.session);
        // send request
        final Future<RpcResult<AddLspOutput>> futureOutput = this.topologyRpcs.addLsp(createAddLspInput());
        this.listener.onSessionDown(this.session, new IllegalArgumentException());
        final AddLspOutput output = futureOutput.get().getResult();
        // deal with unsent request after session down
        assertEquals(FailureType.Unsent, output.getFailure());
    }

    @Test
    public void testOnSessionTermination() throws UnknownHostException, InterruptedException, ExecutionException {
        this.listener.onSessionUp(this.session);

        // create node
        this.topologyRpcs.addLsp(createAddLspInput());
        final Pcinitiate pcinitiate = (Pcinitiate) this.receivedMsgs.get(0);
        final Requests req = pcinitiate.getPcinitiateMessage().getRequests().get(0);
        final long srpId = req.getSrp().getOperationId().getValue();
        final Tlvs tlvs = createLspTlvs(req.getLsp().getPlspId().getValue(), true,
                TEST_ADDRESS, TEST_ADDRESS, TEST_ADDRESS, Optional.<byte[]>absent());
        final Pcrpt pcRpt = MsgBuilderUtil.createPcRtpMessage(new LspBuilder(req.getLsp()).setTlvs(tlvs).setSync(true).setRemove(false).setOperational(OperationalStatus.Active).build(), Optional.of(MsgBuilderUtil.createSrp(srpId)), MsgBuilderUtil.createPath(req.getEro().getSubobject()));
        this.listener.onMessage(this.session, pcRpt);
        assertEquals(1, getTopology().get().getNode().size());

        // node should be removed after termination
        this.listener.onSessionTerminated(this.session, new PCEPCloseTermination(TerminationReason.UNKNOWN));
        assertEquals(0, getTopology().get().getNode().size());
    }

    @Test
    public void testUnknownLsp() throws Exception {
        final List<Reports> reports = Lists.newArrayList(new ReportsBuilder().setPath(new PathBuilder().setEro(new EroBuilder().build()).build()).setLsp(
                new LspBuilder().setPlspId(new PlspId(5L)).setSync(false).setRemove(false).setTlvs(
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.lsp.TlvsBuilder().setLspIdentifiers(new LspIdentifiersBuilder().setLspId(new LspId(1L)).build()).setSymbolicPathName(
                                new SymbolicPathNameBuilder().setPathName(new SymbolicPathName(new byte[] { 22, 34 })).build()).build()).build()).build());
        final Pcrpt rptmsg = new PcrptBuilder().setPcrptMessage(new PcrptMessageBuilder().setReports(reports).build()).build();
        this.listener.onSessionUp(this.session);
        this.listener.onMessage(this.session, rptmsg);
        final Topology topology = getTopology().get();
        assertFalse(topology.getNode().isEmpty());
    }

    @Test
    public void testUpdateUnknownLsp() throws InterruptedException, ExecutionException {
        this.listener.onSessionUp(this.session);
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.update.lsp.args.ArgumentsBuilder updArgsBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.update.lsp.args.ArgumentsBuilder();
        updArgsBuilder.setEro(createEroWithIpPrefixes(Lists.newArrayList(ERO_IP_PREFIX, DST_IP_PREFIX)));
        updArgsBuilder.addAugmentation(Arguments3.class, new Arguments3Builder().setLsp(new LspBuilder().setDelegate(true).setAdministrative(true).build()).build());
        final UpdateLspInput update = new UpdateLspInputBuilder().setArguments(updArgsBuilder.build()).setName(TUNNEL_NAME).setNetworkTopologyRef(new NetworkTopologyRef(TOPO_IID)).setNode(NODE_ID).build();
        final UpdateLspOutput result = this.topologyRpcs.updateLsp(update).get().getResult();
        assertEquals(FailureType.Unsent, result.getFailure());
        assertEquals(1, result.getError().size());
        final ErrorObject errorObject = result.getError().get(0).getErrorObject();
        assertNotNull(errorObject);
        assertEquals(PCEPErrors.UNKNOWN_PLSP_ID, PCEPErrors.forValue(errorObject.getType(), errorObject.getValue()));
    }

    @Test
    public void testRemoveUnknownLsp() throws InterruptedException, ExecutionException {
        this.listener.onSessionUp(this.session);
        final RemoveLspInput remove = new RemoveLspInputBuilder().setName(TUNNEL_NAME).setNetworkTopologyRef(new NetworkTopologyRef(TOPO_IID)).setNode(NODE_ID).build();
        final OperationResult result = this.topologyRpcs.removeLsp(remove).get().getResult();
        assertEquals(FailureType.Unsent, result.getFailure());
        assertEquals(1, result.getError().size());
        final ErrorObject errorObject = result.getError().get(0).getErrorObject();
        assertNotNull(errorObject);
        assertEquals(PCEPErrors.UNKNOWN_PLSP_ID, PCEPErrors.forValue(errorObject.getType(), errorObject.getValue()));
    }

    @Test
    public void testAddAlreadyExistingLsp() throws UnknownHostException, InterruptedException, ExecutionException {
        this.listener.onSessionUp(this.session);
        this.topologyRpcs.addLsp(createAddLspInput());
        assertEquals(1, this.receivedMsgs.size());
        assertTrue(this.receivedMsgs.get(0) instanceof Pcinitiate);
        final Pcinitiate pcinitiate = (Pcinitiate) this.receivedMsgs.get(0);
        final Requests req = pcinitiate.getPcinitiateMessage().getRequests().get(0);
        final long srpId = req.getSrp().getOperationId().getValue();
        final Tlvs tlvs = createLspTlvs(req.getLsp().getPlspId().getValue(), true,
                TEST_ADDRESS, TEST_ADDRESS, TEST_ADDRESS, Optional.<byte[]>absent());
        final Pcrpt pcRpt = MsgBuilderUtil.createPcRtpMessage(new LspBuilder(req.getLsp()).setTlvs(tlvs).setPlspId(new PlspId(1L)).setSync(false).setRemove(false).setOperational(OperationalStatus.Active).build(), Optional.of(MsgBuilderUtil.createSrp(srpId)), MsgBuilderUtil.createPath(req.getEro().getSubobject()));
        this.listener.onMessage(this.session, pcRpt);

        //try to add already existing LSP
        final AddLspOutput result = this.topologyRpcs.addLsp(createAddLspInput()).get().getResult();
        assertEquals(FailureType.Unsent, result.getFailure());
        assertEquals(1, result.getError().size());
        final ErrorObject errorObject = result.getError().get(0).getErrorObject();
        assertNotNull(errorObject);
        assertEquals(PCEPErrors.USED_SYMBOLIC_PATH_NAME, PCEPErrors.forValue(errorObject.getType(), errorObject.getValue()));
    }

    @Override
    protected Open getLocalPref() {
        return new OpenBuilder(super.getLocalPref()).setTlvs(new TlvsBuilder().addAugmentation(Tlvs1.class, new Tlvs1Builder().setStateful(new StatefulBuilder()
            .addAugmentation(Stateful1.class, new Stateful1Builder().setInitiation(Boolean.TRUE).build())
            .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Stateful1.class, new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev150714.Stateful1Builder().setTriggeredInitialSync(Boolean.TRUE).build())
            .build()).build()).build()).build();
    }

    @Override
    protected Open getRemotePref() {
        return getLocalPref();
    }

    private AddLspInput createAddLspInput() {
        final ArgumentsBuilder argsBuilder = new ArgumentsBuilder();
        final Ipv4CaseBuilder ipv4Builder = new Ipv4CaseBuilder();
        ipv4Builder.setIpv4(new Ipv4Builder().setSourceIpv4Address(new Ipv4Address(TEST_ADDRESS)).setDestinationIpv4Address(new Ipv4Address(TEST_ADDRESS)).build());
        argsBuilder.setEndpointsObj(new EndpointsObjBuilder().setAddressFamily(ipv4Builder.build()).build());
        argsBuilder.setEro(createEroWithIpPrefixes(Lists.newArrayList(ERO_IP_PREFIX)));
        argsBuilder.addAugmentation(Arguments2.class, new Arguments2Builder().setLsp(new LspBuilder().setDelegate(true).setAdministrative(true).build()).build());
        return new AddLspInputBuilder().setName(TUNNEL_NAME).setArguments(argsBuilder.build()).setNetworkTopologyRef(new NetworkTopologyRef(TOPO_IID)).setNode(NODE_ID).build();
    }
}
