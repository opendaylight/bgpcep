/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.bgpcep.pcep.topology.provider;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.protocol.pcep.PCEPSessionListener;

interface TopologySessionListener extends PCEPSessionListener, TopologySessionRPCs {
    // FIXME: this needs to provide a future which completes when everything is cleaned up
    ListenableFuture<?> close();
}