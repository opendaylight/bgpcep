/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.session;

import com.google.common.collect.Lists;
import java.util.List;
import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.protocol.bmp.api.BmpSessionListener;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cgasparini on 21.5.2015.
 */
public class BmpTestSessionListener implements BmpSessionListener {

    private static final Logger LOG = LoggerFactory.getLogger(BmpTestSessionListener.class);
    private List<Notification> messages = Lists.newArrayList();
    private boolean up = false;

    public boolean isUp () {
        return this.up;
    }

    public List<Notification> getListMsg() {
        return this.messages;
    }

    @Override
    public void onMessage(final Notification message) {
        LOG.debug("Received message: {} {}", message.getClass(), message);
        this.messages.add(message);
    }

    @Override
    public synchronized void onSessionUp(final BmpSession session) {
        LOG.debug("Session up.");
        this.up = true;
    }

    @Override
    public void onSessionDown(final Exception e) {
        LOG.debug("Session down.", e);
        this.up = false;
    }
}


