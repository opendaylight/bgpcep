/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.testtool;

import org.opendaylight.protocol.bgp.rib.spi.BGPSession;
import org.opendaylight.protocol.bgp.rib.spi.BGPSessionListener;
import org.opendaylight.protocol.bgp.rib.spi.BGPTerminationReason;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeakerSessionListener implements BGPSessionListener {
    private static final Logger LOG = LoggerFactory.getLogger(SpeakerSessionListener.class);

    @Override
    public void onSessionUp(final BGPSession session) {
        LOG.info("Server: Session is up.");
    }

    @Override
    public void onSessionTerminated(final BGPSession session, final BGPTerminationReason cause) {
        LOG.info("Server: Session terminated: {}", cause);
    }

    @Override
    public void onSessionDown(final BGPSession session, final Exception e) {
        LOG.info("Server: Session down.");
        session.close();
        // this.d.stop();
    }

    @Override
    public void onMessage(final BGPSession session, final Notification message) {
        LOG.info("Server: Message received: {}", message);
        // this.d.stop();
    }

    @Override
    public boolean isSessionActive() {
        return true;
    }
}
