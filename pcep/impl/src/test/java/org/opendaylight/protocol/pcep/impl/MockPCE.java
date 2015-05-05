/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.protocol.pcep.PCEPSession;
import org.opendaylight.protocol.pcep.PCEPSessionListener;
import org.opendaylight.protocol.pcep.PCEPTerminationReason;
import org.opendaylight.protocol.pcep.impl.spi.Util;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.open.object.Open;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockPCE implements PCEPSessionListener {

    private final List<Message> listMsg = new ArrayList<Message>();

    private PCEPSessionImpl session = null;

    public boolean up = false;

    private static final Logger LOG = LoggerFactory.getLogger(MockPCE.class);

    public boolean down = false;

    public MockPCE() {
    }

    public void sendMessage(final Message msg) {
        this.session.handleMessage(msg);
    }

    public void sendErrorMessage(final PCEPErrors value, final Open open) {
        this.sendMessage(Util.createErrorMessage(value, open));
    }

    public List<Message> getListMsg() {
        return this.listMsg;
    }

    public void addSession(final PCEPSessionImpl l) {
        this.session = l;
    }

    @Override
    public void onMessage(final PCEPSession session, final Message message) {
        this.listMsg.add(message);
        LOG.debug("Message received: {}", message);
    }

    @Override
    public void onSessionUp(final PCEPSession session) {
        LOG.debug("Session Up");
        this.up = true;
        this.notifyAll();
    }

    @Override
    public void onSessionDown(final PCEPSession session, final Exception e) {
        LOG.debug("Session Down.", e);
        this.down = true;
        // this.notifyAll();
    }

    @Override
    public void onSessionTerminated(final PCEPSession session, final PCEPTerminationReason cause) {
        LOG.debug("Session terminated. Cause : {}", cause);
    }
}
