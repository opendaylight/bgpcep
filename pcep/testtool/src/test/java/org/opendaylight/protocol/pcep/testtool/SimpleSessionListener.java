/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.testtool;

import com.google.common.collect.Lists;
import java.util.List;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPTerminationReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Session Listener that is notified about messages and changes in the session.
 */
public class SimpleSessionListener implements PCEPSessionListener {

    private final List<Message> messages = Lists.newArrayList();

    private static final Logger LOG = LoggerFactory.getLogger(SimpleSessionListener.class);

    SimpleSessionListener() {
    }

    @Override
    public void onMessage(final PCEPSession session, final Message message) {
        LOG.debug("Received message: {}", message);
        this.messages.add(message);
    }

    @Override
    public void onSessionUp(final PCEPSession session) {
        LOG.debug("Session up.");
    }

    @Override
    public void onSessionDown(final PCEPSession session, final Exception e) {
        LOG.debug("Session down with cause : {} or exception: {}", e.getCause(), e, e);
        try {
            session.close();
        } catch (Exception ie) {
            LOG.warn("Error closing session", ie);
        }
    }

    @Override
    public void onSessionTerminated(final PCEPSession session, final PCEPTerminationReason cause) {
        LOG.debug("Session terminated. Cause : {}", cause.toString());
    }
}
