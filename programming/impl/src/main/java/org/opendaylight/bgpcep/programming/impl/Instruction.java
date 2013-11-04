/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import io.netty.util.Timeout;
import io.netty.util.concurrent.Future;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.bgpcep.programming.spi.ExecutionResult;
import org.opendaylight.bgpcep.programming.spi.InstructionExecutor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.instruction.status.changed.Details;

import com.google.common.base.Preconditions;

final class Instruction {
	private final List<Instruction> dependants = new ArrayList<>();
	private final InstructionExecutor executor;
	private final List<Instruction> dependencies;
	private final InstructionId id;
	private volatile InstructionStatus status = InstructionStatus.Queued;
	private Timeout timeout;

	Instruction(final InstructionId id, final InstructionExecutor executor, final List<Instruction> dependencies, final Timeout timeout) {
		this.id = Preconditions.checkNotNull(id);
		this.executor = Preconditions.checkNotNull(executor);
		this.dependencies = Preconditions.checkNotNull(dependencies);
		this.timeout = Preconditions.checkNotNull(timeout);
	}

	InstructionId getId() {
		return id;
	}

	InstructionStatus getStatus() {
		return status;
	}

	Future<ExecutionResult<Details>> execute() {
		return executor.execute();
	}

	void setStatus(final InstructionStatus status) {
		this.status = status;
	}

	synchronized void cancel() {
		if (timeout != null) {
			timeout.cancel();
			timeout = null;
		}
	}

	synchronized void completed() {
		timeout = null;
	}

	synchronized void addDependant(final Instruction d) {
		dependants.add(d);
	}

	List<Instruction> getDependencies() {
		return dependencies;
	}

	List<Instruction> getDependants() {
		return dependants;
	}
}