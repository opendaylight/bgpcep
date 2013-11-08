/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.spi;

import org.opendaylight.bgpcep.programming.spi.ExecutionResult;
import org.opendaylight.bgpcep.programming.spi.InstructionExecutor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.status.changed.Details;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.FailureType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public abstract class AbstractTopologyProgrammingExecutor implements InstructionExecutor {

	protected abstract ListenableFuture<OperationResult> executeImpl();

	@Override
	public final ListenableFuture<ExecutionResult<Details>> execute() {
		return Futures.transform(executeImpl(), new Function<OperationResult, ExecutionResult<Details>>() {

			@Override
			public ExecutionResult<Details> apply(final OperationResult input) {
				final FailureType fail = input.getFailure();
				if (fail == null) {
					return new ExecutionResult<Details>(InstructionStatus.Successful, null);
				}

				switch (fail) {
				case Failed:
				case NoAck:
					return new ExecutionResult<Details>(InstructionStatus.Failed, null);
				case Unsent:
					return new ExecutionResult<Details>(InstructionStatus.Cancelled, null);
				}

				throw new IllegalStateException("Unhandled operation state " + fail);
			}
		});
	}
}
