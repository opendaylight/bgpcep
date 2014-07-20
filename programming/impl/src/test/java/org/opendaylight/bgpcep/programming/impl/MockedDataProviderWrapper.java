/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.Nanotime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.queue.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.queue.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.queue.Instructions;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class MockedDataProviderWrapper {

    private ReadWriteTransaction lastMockedTransaction;
    private DataBroker lastMockedDataProvider;

    private final List<PutOperationalDataInvocationArgs> putOperationalDataInvocations;
    private final List<InstanceIdentifier<?>> removeOperationalDataInvocations;

    MockedDataProviderWrapper() {
        MockitoAnnotations.initMocks(this);
        putOperationalDataInvocations = Lists.newArrayList();
        removeOperationalDataInvocations = Lists.newArrayList();
    }

    public DataBroker getMockedDataProvider() {
        lastMockedTransaction = setupMockedTransaction();
        lastMockedDataProvider = setupMockedDataProvider(lastMockedTransaction);
        return lastMockedDataProvider;
    }

    private DataBroker setupMockedDataProvider(final ReadWriteTransaction mockedTransaction) {
        DataBroker mockedDataProvider = mock(DataBroker.class);
        doReturn(mockedTransaction).when(mockedDataProvider).newReadWriteTransaction();
        doReturn(mockedTransaction).when(mockedDataProvider).newWriteOnlyTransaction();

        ListenableFuture<?> mockedCommitFuture = mock(ListenableFuture.class);
        doReturn(mockedCommitFuture).when(mockedTransaction).commit();
        return mockedDataProvider;
    }

    private ReadWriteTransaction setupMockedTransaction() {
        ReadWriteTransaction mockedRWTransaction = mock(ReadWriteTransaction.class);

        ListenableFuture<?> mockedListenableFuture = Mockito.mock(ListenableFuture.class);
        try {
            doReturn(Optional.absent()).when(mockedListenableFuture).get();
        } catch (InterruptedException | ExecutionException e) {
            // should not happen
        }
        doReturn(mockedListenableFuture).when(mockedRWTransaction).read(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Matchers.<InstanceIdentifier<? extends DataObject>> any());

        setPutAnswer(mockedRWTransaction);
        setRemoveAnwer(mockedRWTransaction);
        return mockedRWTransaction;
    }

    private void setRemoveAnwer(final WriteTransaction mockedTransaction) {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                removeOperationalDataInvocations.add((InstanceIdentifier<?>) invocation.getArguments()[1]);
                return null;
            }
        }).when(mockedTransaction).delete(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Matchers.<InstanceIdentifier<? extends DataObject>> any());
    }

    private void setPutAnswer(final WriteTransaction mockedTransaction) {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                putOperationalDataInvocations.add(PutOperationalDataInvocationArgs.fromObjects(invocation.getArguments()[1],
                        invocation.getArguments()[2]));
                return null;
            }
        }).when(mockedTransaction).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Matchers.<InstanceIdentifier<? extends DataObject>> any(), any(DataObject.class));
    }

    MockedDataProviderWrapper verifyBeginWriteTransaction(final int beginTransactionCount) {
        verify(lastMockedDataProvider, times(beginTransactionCount)).newWriteOnlyTransaction();
        return this;
    }

    MockedDataProviderWrapper verifyBeginRWTransaction(final int beginTransactionCount) {
        verify(lastMockedDataProvider, times(beginTransactionCount)).newReadWriteTransaction();
        return this;
    }

    MockedDataProviderWrapper verifyPutDataOnTransaction(final int putCount) {
        verify(lastMockedTransaction, times(putCount)).put(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Matchers.<InstanceIdentifier<? extends DataObject>> any(),
                any(DataObject.class));
        return this;
    }

    MockedDataProviderWrapper verifyRemoveDataOnTransaction(final int removeCount) {
        verify(lastMockedTransaction, times(removeCount)).delete(Mockito.eq(LogicalDatastoreType.OPERATIONAL), Matchers.<InstanceIdentifier<? extends DataObject>> any());
        return this;
    }

    MockedDataProviderWrapper verifyCommitTransaction(final int commitCount) {
        verify(lastMockedTransaction, times(commitCount)).commit();
        return this;
    }

    void assertPutDataForInstructions(final int idx, final InstructionId expectedId, final Nanotime expectedDeadline) {
        assertPutDataTargetType(idx, Instruction.class);
        assertEquals(Instructions.class, putOperationalDataInvocations.get(idx).data.getImplementedInterface());
        Instructions instructions = (Instructions) putOperationalDataInvocations.get(idx).data;
        assertEquals(expectedId, instructions.getId());
        if (expectedDeadline != null) {
            assertEquals(expectedDeadline, instructions.getDeadline());
        }
    }

    public void assertPutDataTargetType(final int idx, final Class<?> targetType) {
        assertEquals(targetType, putOperationalDataInvocations.get(idx).id.getTargetType());
    }

    public void assertRemoveDataForInstruction(final int idx, final InstructionId expectedId) {
        assertEquals(Instruction.class, removeOperationalDataInvocations.get(idx).getTargetType());
        InstanceIdentifier<? extends DataObject> instanceId = removeOperationalDataInvocations.get(idx).firstIdentifierOf(Instruction.class);
        assertNotNull(instanceId);

        InstanceIdentifier.PathArgument instructionPathArg = Iterables.get(instanceId.getPathArguments(), 1);
        assertTrue(instructionPathArg instanceof InstanceIdentifier.IdentifiableItem);
        InstructionKey expectedKey = new InstructionKey(expectedId);
        assertEquals(expectedKey, ((InstanceIdentifier.IdentifiableItem<?, ?>) instructionPathArg).getKey());
    }

    static final class PutOperationalDataInvocationArgs {
        private final InstanceIdentifier<?> id;
        private final DataObject data;

        private PutOperationalDataInvocationArgs(final InstanceIdentifier<?> id, final DataObject data) {
            this.id = id;
            this.data = data;
        }

        static PutOperationalDataInvocationArgs fromObjects(final Object id, final Object data) {
            Preconditions.checkArgument(id instanceof InstanceIdentifier<?>);
            Preconditions.checkArgument(data instanceof DataObject);
            return new PutOperationalDataInvocationArgs((InstanceIdentifier<?>) id, (DataObject) data);
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("PutOperationalDataInvocationArgs{");
            sb.append("id=").append(id);
            sb.append(", data=").append(data);
            sb.append('}');
            return sb.toString();
        }
    }
}
