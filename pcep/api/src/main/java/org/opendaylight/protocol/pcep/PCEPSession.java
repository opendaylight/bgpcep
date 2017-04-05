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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.stats.rev141006.PcepSessionState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.open.Tlvs;

/**
 * PCEP Session represents the finite state machine in PCEP, including timers and its purpose is to create a PCEP
 * connection between PCE/PCC. Session is automatically started, when TCP connection is created, but can be stopped
 * manually. If the session is up, it has to redirect messages to/from user. Handles also malformed messages and unknown
 * requests.
 */
public interface PCEPSession extends AutoCloseable, PcepSessionState {

    /**
     * Sends message from user to PCE/PCC. If the user sends an Open Message, the session returns an error (open message
     * is only allowed, when a PCEP handshake is in progress). Close message will close the session and free all the
     * resources.
     *
     * @param message message to be sent
     * @return Future promise which will be succeed when the message is enqueued in the socket.
     */
    Future<Void> sendMessage(Message message);

    /**
     * Closes PCEP session, cancels all timers, returns to state Idle, sends the Close Message. KeepAlive and DeadTimer
     * are cancelled if the state of the session changes to IDLE. This method is used to close the PCEP session from
     * inside the session or from the listener, therefore the parent of this session should be informed.
     * @param reason The {@link TerminationReason} to be wrapped in a PCEP CLOSE message and sent to the remote peer.
     *               When the reason provided is null, no CLOSE message will be sent.
     */
    void close(@Nullable TerminationReason reason);

    /**
     * Terminate a PCEP session.  A CLOSE message with given reason will be sent to remote peer by invoking
     * {@link #close(TerminationReason)} method.
     *
     * It triggers {@link PCEPSessionListener#onSessionTerminated(PCEPSession, PCEPTerminationReason)} after closing.
     * @param reason The {@link TerminationReason} to be wrapped in a PCEP CLOSE message and sent to the remote peer.
     *               The reason cannot be null.
     */
    void terminate(@Nonnull TerminationReason reason);

    Tlvs getRemoteTlvs();

    InetAddress getRemoteAddress();

    void resetStats();

    /**
     * Returns session characteristics of the local PCEP Speaker
     * @return Open message TLVs
     */
    Tlvs localSessionCharacteristics();
}
