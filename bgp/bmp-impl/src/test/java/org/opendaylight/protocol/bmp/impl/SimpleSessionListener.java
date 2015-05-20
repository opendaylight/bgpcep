/*
 *
 *  * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.protocol.bmp.impl;

import com.google.common.collect.Lists;

import java.util.List;

import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.protocol.bmp.api.BmpSessionListener;
import org.opendaylight.protocol.bmp.api.BmpTerminationReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgasparini on 21.5.2015.
 */
public class SimpleSessionListener implements BmpSessionListener {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleSessionListener.class);
    public List<Notification> messages = Lists.newArrayList();
    public boolean up = false;

    public SimpleSessionListener() {
    }

    public List<Notification> getListMsg() {
        return this.messages;
    }

    @Override
    public void onMessage(final BmpSession session, final Notification message) {
        LOG.debug("Received message: {} {}", message.getClass(), message);
        this.messages.add(message);
    }

    @Override
    public synchronized void onSessionUp(final BmpSession session) {
        LOG.debug("Session up.");
        this.up = true;
        this.notifyAll();
    }

    @Override
    public void onSessionDown(final BmpSession session, final Exception e) {
        LOG.debug("Session down.", e);
        this.up = false;
    }

    @Override
    public void onSessionTerminated(final BmpSession bmpSession, final BmpTerminationReason cause) {
        LOG.debug("Session terminated. Cause : {}", cause.toString());
    }

    @Override
    public boolean isSessionActive() {
        return true;
    }

    @Override
    public void markUptodate(final TablesKey tablesKey) {
        LOG.debug("Table marked as up-to-date {}", tablesKey);
    }
}


