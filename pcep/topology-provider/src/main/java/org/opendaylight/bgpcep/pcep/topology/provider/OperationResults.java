/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.FailureType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yangtools.yang.binding.DataContainer;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 *
 */
enum OperationResults implements OperationResult {
	NOACK {
		@Override
		public FailureType getFailure() {
			return FailureType.NoAck;
		}

	},
	SUCCESS {
		@Override
		public FailureType getFailure() {
			return null;
		}
	},
	UNSENT {
		@Override
		public FailureType getFailure() {
			return FailureType.Unsent;
		}
	};

	@Override
	public Class<? extends DataContainer> getImplementedInterface() {
		return OperationResult.class;
	}

	ListenableFuture<OperationResult> future() {
		return Futures.<OperationResult>immediateFuture(this);
	}
}
