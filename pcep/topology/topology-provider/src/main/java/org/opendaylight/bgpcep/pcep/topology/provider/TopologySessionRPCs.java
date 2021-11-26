/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.util.concurrent.ListenableFuture;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.AddLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.RemoveLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.TearDownSessionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.TriggerSyncArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev220730.UpdateLspArgs;
import org.opendaylight.yangtools.yang.common.RpcResult;

interface TopologySessionRPCs {
    @NonNull ListenableFuture<OperationResult> addLsp(AddLspArgs input);

    @NonNull ListenableFuture<OperationResult> removeLsp(RemoveLspArgs input);

    @NonNull ListenableFuture<OperationResult> updateLsp(UpdateLspArgs input);

    @NonNull ListenableFuture<OperationResult> ensureLspOperational(EnsureLspOperationalInput input);

    @NonNull ListenableFuture<OperationResult> triggerSync(TriggerSyncArgs input);

    @NonNull ListenableFuture<RpcResult<Void>> tearDownSession(TearDownSessionInput input);
}