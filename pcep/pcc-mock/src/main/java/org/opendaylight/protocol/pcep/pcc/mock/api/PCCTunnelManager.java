/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock.api;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev181109.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.initiated.rev181109.pcinitiate.message.pcinitiate.message.Requests;

public interface PCCTunnelManager {

    void onSessionUp(PCCSession session);

    void onSessionDown(PCCSession session);

    void onMessagePcupd(@NonNull Updates update, @NonNull PCCSession session);

    void onMessagePcInitiate(@NonNull Requests request, @NonNull PCCSession session);
}
