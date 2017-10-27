/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import java.util.EventListener;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Listener that receives session informations from the session.
 */
public interface BGPSessionListener extends PeerRPCs, EventListener {

    /**
     * Marks synchronization finished for given Table key
     *
     * @param tablesKey of the table where synchronization finished
     */
    void markUptodate(final TablesKey tablesKey);

    /**
     * Fired when the session was established successfully.
     *
     * @param session Peer address families which we accepted
     */
    void onSessionUp(BGPSession session);
    /**
     * Fired when the session went down because of an IO error. Implementation should take care of closing underlying
     * session.
     *
     * @param session that went down
     * @param e Exception that was thrown as the cause of session being down
     */

    void onSessionDown(BGPSession session, Exception e);
    /**
     * Fired when the session is terminated locally. The session has already been closed and transitioned to IDLE state.
     * Any outstanding queued messages were not sent. The user should not attempt to make any use of the session.
     *
     * @param reason the cause why the session went down
     */
    void onSessionTerminated(BGPSession session, BGPTerminationReason reason);

    /**
     * Fired when a normal protocol message is received.
     *
     * @param notification Protocol message
     */
    void onMessage(BGPSession session, Notification notification) throws BGPDocumentedException;
}
