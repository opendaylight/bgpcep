/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Factory for creating PCEP session negotiator.
 *
 * @param <S> PCEPSession implementation
 */
public interface PCEPSessionNegotiatorFactory<S extends PCEPSession> {

    /**
     * Creates PCEPSessionNegotiator instance for income attributes.
     *
     * @param sessionNegotiatorDependencies contains PCEPSessionNegotiator dependencies
     * @param channel                       session channel
     * @param promise                       session promise
     * @return PCEPSessionNegotiator instance
     */
    @NonNull SessionNegotiator getSessionNegotiator(
            @NonNull PCEPSessionNegotiatorFactoryDependencies sessionNegotiatorDependencies,
            @NonNull Channel channel, @NonNull Promise<S> promise);

    /**
     * Returns a PCEPSessionProposalFactory.
     *
     * @return session factory
     */
    @NonNull PCEPSessionProposalFactory getPCEPSessionProposalFactory();
}
