/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.impl.session;

import io.netty.channel.Channel;
import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.protocol.bmp.api.BmpSessionFactory;
import org.opendaylight.protocol.bmp.api.BmpSessionListenerFactory;

public final class DefaultBmpSessionFactory implements BmpSessionFactory {

    @Override
    public BmpSession getSession(final Channel channel, final BmpSessionListenerFactory sessionListenerFactory) {
        return new BmpSessionImpl(sessionListenerFactory.getSessionListener());
    }

}
