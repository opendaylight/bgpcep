/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.programming.impl;

import io.netty.util.Timeout;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.InstructionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.programming.rev130930.SubmitInstructionInput;

import com.google.common.base.Preconditions;

final class Instruction {
	private final List<Instruction> dependants = new ArrayList<>();
	private final List<Instruction> dependencies;
	private final SubmitInstructionInput input;
	private volatile InstructionStatus status = InstructionStatus.Queued;
	private Timeout timeout;

	Instruction(final SubmitInstructionInput input, final List<Instruction> dependencies, final Timeout timeout) {
		this.input = Preconditions.checkNotNull(input);
		this.dependencies = Preconditions.checkNotNull(dependencies);
		this.timeout = Preconditions.checkNotNull(timeout);
	}

	SubmitInstructionInput getInput() {
		return input;
	}

	InstructionStatus getStatus() {
		return status;
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