/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.bgpcep.pcep.topology.provider.TopologyProgrammingTest.MockedTopologySessionListenerFactory;
import org.opendaylight.bgpcep.programming.spi.Instruction;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.SubmitInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.instruction.status.changed.Details;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev171025.SubmitAddLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev171025.SubmitEnsureLspOperationalInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev171025.SubmitRemoveLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev171025.SubmitTriggerSyncInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev171025.SubmitUpdateLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.AddLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.AddLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.AddLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.EnsureLspOperationalOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.RemoveLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.RemoveLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.RemoveLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.TriggerSyncArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.TriggerSyncInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.TriggerSyncOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.UpdateLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.UpdateLspInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.UpdateLspOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.ensure.lsp.operational.args.ArgumentsBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class TopologyProgrammingTest extends AbstractPCEPSessionTest<MockedTopologySessionListenerFactory> {

    private static final String NAME = "test-tunnel";
    private static Stateful07TopologySessionListener listener;

    @Mock
    private InstructionScheduler scheduler;
    @Mock
    private ListenableFuture<Instruction> instructionFuture;
    @Mock
    private Instruction instruction;

    private AddLspArgs addLspArgs;
    private UpdateLspArgs updateLspArgs;
    private RemoveLspArgs removeLspArgs;
    private TriggerSyncArgs triggerSyncArgs;
    private EnsureLspOperationalInput ensureLspInput;

    @Mock
    private ListenableFuture<RpcResult<AddLspOutput>> futureAddLspOutput;
    @Mock
    private ListenableFuture<RpcResult<UpdateLspOutput>> futureUpdateLspOutput;
    @Mock
    private ListenableFuture<RpcResult<RemoveLspOutput>> futureRemoveLspOutput;
    @Mock
    private ListenableFuture<RpcResult<TriggerSyncOutput>> futureTriggerSyncOutput;
    @Mock
    private ListenableFuture<RpcResult<EnsureLspOperationalOutput>> futureEnsureLspOutput;

    private TopologyProgramming topologyProgramming;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        Mockito.doReturn(true).when(this.instruction).checkedExecutionStart();
        Mockito.doNothing().when(this.instruction).executionCompleted(InstructionStatus.Failed, null);
        Mockito.doAnswer(invocation -> {
            final Runnable callback = (Runnable) invocation.getArguments()[0];
            callback.run();
            return null;
        }).when(this.instructionFuture).addListener(Mockito.any(Runnable.class), Mockito.any(Executor.class));
        Mockito.doAnswer(invocation -> {
            final Runnable callback = (Runnable) invocation.getArguments()[0];
            callback.run();
            return null;
        }).when(this.futureAddLspOutput).addListener(Mockito.any(Runnable.class), Mockito.any(Executor.class));
        Mockito.doAnswer(invocation -> {
            final Runnable callback = (Runnable) invocation.getArguments()[0];
            callback.run();
            return null;
        }).when(this.futureUpdateLspOutput).addListener(Mockito.any(Runnable.class), Mockito.any(Executor.class));
        Mockito.doAnswer(invocation -> {
            final Runnable callback = (Runnable) invocation.getArguments()[0];
            callback.run();
            return null;
        }).when(this.futureRemoveLspOutput).addListener(Mockito.any(Runnable.class), Mockito.any(Executor.class));
        Mockito.doAnswer(invocation -> {
            final Runnable callback = (Runnable) invocation.getArguments()[0];
            callback.run();
            return null;
        }).when(this.futureTriggerSyncOutput).addListener(Mockito.any(Runnable.class), Mockito.any(Executor.class));
        Mockito.doAnswer(invocation -> {
            final Runnable callback = (Runnable) invocation.getArguments()[0];
            callback.run();
            return null;
        }).when(this.futureEnsureLspOutput).addListener(Mockito.any(Runnable.class), Mockito.any(Executor.class));
        Mockito.doAnswer(invocation -> {
            TopologyProgrammingTest.this.addLspArgs = (AddLspArgs) invocation.getArguments()[0];
            return TopologyProgrammingTest.this.futureAddLspOutput;
        }).when(listener).addLsp(Mockito.any(AddLspInput.class));
        Mockito.doAnswer(invocation -> {
            TopologyProgrammingTest.this.updateLspArgs = (UpdateLspArgs) invocation.getArguments()[0];
            return TopologyProgrammingTest.this.futureUpdateLspOutput;
        }).when(listener).updateLsp(Mockito.any(UpdateLspInput.class));
        Mockito.doAnswer(invocation -> {
            TopologyProgrammingTest.this.removeLspArgs = (RemoveLspArgs) invocation.getArguments()[0];
            return TopologyProgrammingTest.this.futureRemoveLspOutput;
        }).when(listener).removeLsp(Mockito.any(RemoveLspInput.class));
        Mockito.doAnswer(invocation -> {
            TopologyProgrammingTest.this.triggerSyncArgs = (TriggerSyncArgs) invocation.getArguments()[0];
            return TopologyProgrammingTest.this.futureTriggerSyncOutput;
        }).when(listener).triggerSync(Mockito.any(TriggerSyncInput.class));
        Mockito.doAnswer(invocation -> {
            TopologyProgrammingTest.this.ensureLspInput = (EnsureLspOperationalInput) invocation.getArguments()[0];
            return TopologyProgrammingTest.this.futureEnsureLspOutput;
        }).when(listener).ensureLspOperational(Mockito.any(EnsureLspOperationalInput.class));
        Mockito.doNothing().when(listener).close();
        Mockito.doReturn(this.instruction).when(this.instructionFuture).get();
        Mockito.doReturn(true).when(this.instructionFuture).isDone();
        Mockito.doNothing().when(this.instruction).executionCompleted(Mockito.any(InstructionStatus.class), Mockito.any(Details.class));
        Mockito.doReturn(this.instructionFuture).when(this.scheduler).scheduleInstruction(Mockito.any(SubmitInstructionInput.class));
        this.topologyProgramming = new TopologyProgramming(this.scheduler, this.manager);
        final PCEPSession session = getPCEPSession(getLocalPref(), getRemotePref());
        listener.onSessionUp(session);
    }

    @Test
    public void testSubmitAddLsp() throws InterruptedException, ExecutionException {
        final SubmitAddLspInputBuilder inputBuilder = new SubmitAddLspInputBuilder();
        inputBuilder.setName(NAME);
        inputBuilder.setNode(this.nodeId);
        inputBuilder.setArguments(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.add.lsp.args.ArgumentsBuilder().build());
        this.topologyProgramming.submitAddLsp(inputBuilder.build());
        Assert.assertNotNull(this.addLspArgs);
        Assert.assertEquals(NAME, this.addLspArgs.getName());
        Assert.assertEquals(this.nodeId, this.addLspArgs.getNode());
    }

    @Test
    public void testSubmitUpdateLsp() {
        final SubmitUpdateLspInputBuilder inputBuilder = new SubmitUpdateLspInputBuilder();
        inputBuilder.setName(NAME);
        inputBuilder.setNode(this.nodeId);
        this.topologyProgramming.submitUpdateLsp(inputBuilder.build());
        Assert.assertNotNull(this.updateLspArgs);
        Assert.assertEquals(NAME, this.updateLspArgs.getName());
        Assert.assertEquals(this.nodeId, this.updateLspArgs.getNode());
    }

    @Test
    public void testSubmitEnsureLsp() {
        final SubmitEnsureLspOperationalInputBuilder inputBuilder = new SubmitEnsureLspOperationalInputBuilder();
        inputBuilder.setName(NAME);
        inputBuilder.setNode(this.nodeId);
        inputBuilder.setArguments(new ArgumentsBuilder().build());
        this.topologyProgramming.submitEnsureLspOperational(inputBuilder.build());
        Assert.assertNotNull(this.ensureLspInput);
        Assert.assertEquals(NAME, this.ensureLspInput.getName());
        Assert.assertEquals(this.nodeId, this.ensureLspInput.getNode());
    }

    @Test
    public void testSubmitRemoveLsp() {
        final SubmitRemoveLspInputBuilder inputBuilder = new SubmitRemoveLspInputBuilder();
        inputBuilder.setName(NAME);
        inputBuilder.setNode(this.nodeId);
        this.topologyProgramming.submitRemoveLsp(inputBuilder.build());
        Assert.assertNotNull(this.removeLspArgs);
        Assert.assertEquals(NAME, this.removeLspArgs.getName());
        Assert.assertEquals(this.nodeId, this.removeLspArgs.getNode());
    }

    @Test
    public void testSubmitTriggerSync() {
        final SubmitTriggerSyncInputBuilder inputBuilder = new SubmitTriggerSyncInputBuilder();
        inputBuilder.setName(NAME);
        inputBuilder.setNode(this.nodeId);
        this.topologyProgramming.submitTriggerSync(inputBuilder.build());
        Assert.assertNotNull(this.triggerSyncArgs);
        Assert.assertEquals(NAME, this.triggerSyncArgs.getName());
        Assert.assertEquals(this.nodeId, this.triggerSyncArgs.getNode());
    }

    protected static final class MockedTopologySessionListenerFactory implements TopologySessionListenerFactory {
        @Override
        public TopologySessionListener createTopologySessionListener(final ServerSessionManager manager) {
            listener = Mockito.spy(new Stateful07TopologySessionListener(manager));
            return listener;
        }
    }

    @Override
    protected Open getLocalPref() {
        return new OpenBuilder(super.getLocalPref()).setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.TlvsBuilder().addAugmentation(Tlvs1.class, new Tlvs1Builder().setStateful(new StatefulBuilder().build()).build()).build()).build();
    }
}
