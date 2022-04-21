/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.opendaylight.bgpcep.programming.spi.Instruction;
import org.opendaylight.bgpcep.programming.spi.InstructionScheduler;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev200720.stateful.capability.tlv.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.SubmitInstructionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev150720.instruction.status.changed.Details;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitAddLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitEnsureLspOperationalInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitRemoveLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitTriggerSyncInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.programming.rev181109.SubmitUpdateLspInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.AddLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.RemoveLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.TriggerSyncArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.UpdateLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120.ensure.lsp.operational.args.ArgumentsBuilder;

public class TopologyProgrammingTest extends AbstractPCEPSessionTest {
    private static final String NAME = "test-tunnel";

    @Mock
    private InstructionScheduler scheduler;
    @Mock
    private Instruction instruction;

    private TopologyProgramming topologyProgramming;
    private PCEPTopologySessionListener listener;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        doNothing().when(listener).close();

        doReturn(true).when(instruction).checkedExecutionStart();
        doNothing().when(instruction).executionCompleted(InstructionStatus.Failed, null);
        doNothing().when(instruction).executionCompleted(any(InstructionStatus.class), any(Details.class));

        doReturn(Futures.immediateFuture(instruction)).when(scheduler)
            .scheduleInstruction(any(SubmitInstructionInput.class));
        topologyProgramming = new TopologyProgramming(scheduler, manager);
        final PCEPSession session = getPCEPSession(getLocalPref(), getRemotePref());
        listener.onSessionUp(session);
    }

    @Test
    public void testSubmitAddLsp() {
        final var captor = ArgumentCaptor.forClass(AddLspArgs.class);
        doReturn(Futures.immediateFuture(OperationResults.SUCCESS)).when(listener).addLsp(captor.capture());

        topologyProgramming.submitAddLsp(new SubmitAddLspInputBuilder()
            .setName(NAME).setNode(nodeId)
            .setArguments(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev200120
                .add.lsp.args.ArgumentsBuilder().build())
            .build());

        final var addLspArgs = captor.getValue();
        assertNotNull(addLspArgs);
        assertEquals(NAME, addLspArgs.getName());
        assertEquals(nodeId, addLspArgs.getNode());
    }

    @Test
    public void testSubmitUpdateLsp() {
        final var captor = ArgumentCaptor.forClass(UpdateLspArgs.class);
        doReturn(Futures.immediateFuture(OperationResults.SUCCESS)).when(listener).updateLsp(captor.capture());

        topologyProgramming.submitUpdateLsp(new SubmitUpdateLspInputBuilder().setName(NAME).setNode(nodeId).build());

        final var updateLspArgs = captor.getValue();
        assertNotNull(updateLspArgs);
        assertEquals(NAME, updateLspArgs.getName());
        assertEquals(nodeId, updateLspArgs.getNode());
    }

    @Test
    public void testSubmitEnsureLsp() {
        final var captor = ArgumentCaptor.forClass(EnsureLspOperationalInput.class);
        doReturn(Futures.immediateFuture(OperationResults.SUCCESS)).when(listener)
            .ensureLspOperational(captor.capture());

        topologyProgramming.submitEnsureLspOperational(new SubmitEnsureLspOperationalInputBuilder()
            .setName(NAME)
            .setNode(nodeId)
            .setArguments(new ArgumentsBuilder().build())
            .build());

        final var ensureLspInput = captor.getValue();
        assertNotNull(ensureLspInput);
        assertEquals(NAME, ensureLspInput.getName());
        assertEquals(nodeId, ensureLspInput.getNode());
    }

    @Test
    public void testSubmitRemoveLsp() {
        final var captor = ArgumentCaptor.forClass(RemoveLspArgs.class);
        doReturn(Futures.immediateFuture(OperationResults.SUCCESS)).when(listener).removeLsp(captor.capture());

        topologyProgramming.submitRemoveLsp(new SubmitRemoveLspInputBuilder().setName(NAME).setNode(nodeId).build());

        final var removeLspArgs = captor.getValue();
        assertNotNull(removeLspArgs);
        assertEquals(NAME, removeLspArgs.getName());
        assertEquals(nodeId, removeLspArgs.getNode());
    }

    @Test
    public void testSubmitTriggerSync() {
        final var captor = ArgumentCaptor.forClass(TriggerSyncArgs.class);
        doReturn(Futures.immediateFuture(OperationResults.SUCCESS)).when(listener).triggerSync(captor.capture());

        topologyProgramming.submitTriggerSync(new SubmitTriggerSyncInputBuilder()
            .setName(NAME)
            .setNode(nodeId)
            .build());

        final var triggerSyncArgs = captor.getValue();
        assertNotNull(triggerSyncArgs);
        assertEquals(NAME, triggerSyncArgs.getName());
        assertEquals(nodeId, triggerSyncArgs.getNode());
    }

    @Override
    protected Open getLocalPref() {
        return new OpenBuilder(super.getLocalPref()).setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params
                .xml.ns.yang.pcep.types.rev181109.open.object.open.TlvsBuilder().addAugmentation(
                    new Tlvs1Builder().setStateful(new StatefulBuilder().build()).build()).build()).build();
    }

    @Override
    ServerSessionManager customizeSessionManager(final ServerSessionManager original) {
        final var customized = spy(original);
        listener = spy(new PCEPTopologySessionListener(original));
        doReturn(listener).when(customized).getSessionListener();
        return customized;
    }
}
