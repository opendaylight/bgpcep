/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import io.netty.util.concurrent.Future;
import java.net.InetAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.Messages;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.stats.rev171113.pcep.session.state.PeerPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.Tlvs;

/**
 * PCEP Session represents the finite state machine in PCEP, including timers and its purpose is to create a PCEP
 * connection between PCE/PCC. Session is automatically started, when TCP connection is created, but can be stopped
 * manually. If the session is up, it has to redirect messages to/from user. Handles also malformed messages and unknown
 * requests.
 */
public interface PCEPSession extends PCEPSessionState, AutoCloseable {

    /**
     * Sends message from user to PCE/PCC. If the user sends an Open Message, the session returns an error (open message
     * is only allowed, when a PCEP handshake is in progress). Close message will close the session and free all the
     * resources.
     *
     * @param message message to be sent
     * @return Future promise which will be succeed when the message is enqueued in the socket.
     */
    Future<Void> sendMessage(Message message);

    void close(TerminationReason reason);

    Tlvs getRemoteTlvs();

    InetAddress getRemoteAddress();

    /**
     * Returns session characteristics of the local PCEP Speaker
     *
     * @return Open message TLVs
     */
    Tlvs localSessionCharacteristics();
}
