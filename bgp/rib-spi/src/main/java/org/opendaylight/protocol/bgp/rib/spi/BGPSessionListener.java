/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import java.util.EventListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Listener that receives session informations from the session.
 */
public interface BGPSessionListener extends EventListener {

    /**
     * Returns state of BGP session associated with this listener.
     *
     * @return false if session associated with this listener is null, true if its non-null
     */
    boolean isSessionActive();

    /**
     * Marks synchronization finished for given Table key
     *
     * @param tablesKey of the table where synchronization finished
     */
    void markUptodate(final TablesKey tablesKey);

    void onSessionUp(BGPSession var1);

    void onSessionDown(BGPSession var1, Exception var2);

    void onSessionTerminated(BGPSession var1, BGPTerminationReason var2);

    void onMessage(BGPSession var1, Notification var2);
}
