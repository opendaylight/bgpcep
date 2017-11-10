/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.util.concurrent.ListenableFuture;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.topology.pcep.rpc.rev171110.ReleaseConnectionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.AddLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.EnsureLspOperationalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.OperationResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.RemoveLspArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.TriggerSyncArgs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.topology.pcep.rev131024.UpdateLspArgs;

interface TopologySessionRPCs {
    ListenableFuture<OperationResult> addLsp(AddLspArgs input);

    ListenableFuture<OperationResult> removeLsp(RemoveLspArgs input);

    ListenableFuture<OperationResult> updateLsp(UpdateLspArgs input);

    ListenableFuture<OperationResult> ensureLspOperational(EnsureLspOperationalInput input);

    ListenableFuture<OperationResult> triggerSync(TriggerSyncArgs input);

    ListenableFuture<Void> releaseSession(ReleaseConnectionInput input);
}