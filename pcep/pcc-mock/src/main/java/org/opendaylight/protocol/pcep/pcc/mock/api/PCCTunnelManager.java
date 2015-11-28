/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock.api;

import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.Updates;

public interface PCCTunnelManager {

    void reportToAll(Updates updates, PCCSession session);

    void returnDelegation(Updates updates, PCCSession session);

    void takeDelegation(Requests request, PCCSession session);

    void onSessionUp(PCCSession session);

    void onSessionDown(PCCSession session);

    void addTunnel(Requests request, PCCSession session);

    void removeTunnel(Requests request, PCCSession session);

    void onMessagePcupd(@Nonnull Updates update, @Nonnull final PCCSession session);

    void onMesssgePcinitiate(@Nonnull Requests request, @Nonnull PCCSession session);
}
