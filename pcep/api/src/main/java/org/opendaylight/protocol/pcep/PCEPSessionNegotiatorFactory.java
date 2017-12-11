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
import javax.annotation.Nonnull;

/**
 * Factory for creating PCEP session negotiator.
 *
 * @param <S>
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
    @Nonnull
    SessionNegotiator getSessionNegotiator(
            @Nonnull PCEPSessionNegotiatorFactoryDependencies sessionNegotiatorDependencies,
            @Nonnull Channel channel,
            @Nonnull Promise<S> promise);

    /**
     * Returns a PCEPSessionProposalFactory
     *
     * @return session factory
     */
    @Nonnull
    PCEPSessionProposalFactory getPCEPSessionProposalFactory();
}
