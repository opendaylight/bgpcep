/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bmp.mock;

import io.netty.channel.Channel;
import org.opendaylight.protocol.bmp.api.BmpSession;
import org.opendaylight.protocol.bmp.api.BmpSessionFactory;
import org.opendaylight.protocol.bmp.api.BmpSessionListenerFactory;

public final class BmpMockSessionFactory implements BmpSessionFactory {
    private final BmpMockArguments arguments;

    public BmpMockSessionFactory(final BmpMockArguments arguments) {
        this.arguments = arguments;
    }

    @Override
    public BmpSession getSession(final Channel channel, final BmpSessionListenerFactory sessionListenerFactory) {
        return new BmpMockSession(this.arguments.getPeersCount(),
                this.arguments.getPrePolicyRoutesCount(), this.arguments.getPostPolicyRoutesCount());
    }
}
