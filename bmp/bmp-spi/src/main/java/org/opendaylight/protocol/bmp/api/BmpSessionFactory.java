/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.api;

import io.netty.channel.Channel;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public interface BmpSessionFactory {
    /**
     * Creates Bmp Session.
     *
     * @param channel                generated channel
     * @param sessionListenerFactory listener factory
     * @return bmp session
     */
    BmpSession getSession(Channel channel, BmpSessionListenerFactory sessionListenerFactory);
}
