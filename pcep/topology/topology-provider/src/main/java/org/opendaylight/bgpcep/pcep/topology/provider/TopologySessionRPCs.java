/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.util.concurrent.ListenableFuture;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.AddLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.RemoveLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.TearDownSessionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.TriggerSyncArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev171025.UpdateLspArgs;
import org.opendaylight.yangtools.yang.common.RpcResult;

interface TopologySessionRPCs {
    @Nonnull
    ListenableFuture<OperationResult> addLsp(AddLspArgs input);

    @Nonnull
    ListenableFuture<OperationResult> removeLsp(RemoveLspArgs input);

    @Nonnull
    ListenableFuture<OperationResult> updateLsp(UpdateLspArgs input);

    @Nonnull
    ListenableFuture<OperationResult> ensureLspOperational(EnsureLspOperationalInput input);

    @Nonnull
    ListenableFuture<OperationResult> triggerSync(TriggerSyncArgs input);

    @Nonnull
    ListenableFuture<RpcResult<Void>> tearDownSession(TearDownSessionInput input);
}