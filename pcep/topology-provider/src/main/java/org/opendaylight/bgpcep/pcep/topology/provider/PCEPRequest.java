/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.lsp.metadata.Metadata;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

final class PCEPRequest {
	private final SettableFuture<OperationResult> future;
	private final Metadata metadata;

	PCEPRequest(final Metadata metadata) {
		this.future = SettableFuture.create();
		this.metadata = metadata;
	}

	protected ListenableFuture<OperationResult> getFuture() {
		return future;
	}

	public void setResult(final OperationResult result) {
		future.set(result);
	}

	public Metadata getMetadata() {
		return metadata;
	}
}