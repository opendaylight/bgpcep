/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock.api;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev131126.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.Updates;

public interface PccTunnelManager {

    void reportToAll(Updates updates, PccSession session);

    void returnDelegation(Updates updates, PccSession session);

    void takeDelegation(Requests request, PccSession session);

    void onSessionUp(PccSession session);

    void onSessionDown(PccSession session);

    void addTunnel(Requests request, PccSession session);

    void removeTunnel(Requests request, PccSession session);

}
