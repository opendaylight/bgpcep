/*
 *
 *  * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.protocol.bgp.rib.protocol;

import io.netty.util.concurrent.Future;

/**
 * Created by cgasparini on 19.6.2015.
 */
public interface ReconnectStrategy {
    int getConnectTimeout() throws Exception;

    Future<Void> scheduleReconnect(Throwable var1);

    void reconnectSuccessful();
}
