/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import org.opendaylight.protocol.framework.SessionListener;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Listener that receives session informations from the session.
 */
public interface BGPSessionListener extends SessionListener<Notification, BGPSession, BGPTerminationReason> {

    boolean isSessionActive();
}
