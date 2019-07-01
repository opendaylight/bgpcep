/*
 * Copyright (c) 2017 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Contains required dependencies for create SessionNegotiator.
 */
public interface PCEPSessionNegotiatorFactoryDependencies {
    /**
     * ListenerFactory to create listeners for clients.
     *
     * @return ListenerFactory
     */
    @NonNull PCEPSessionListenerFactory getListenerFactory();

    /**
     * PeerProposal information used in our Open message.
     *
     * @return peerProposal null by default since its not mandatory. Otherwise method should be override it.
     */
    default @Nullable PCEPPeerProposal getPeerProposal() {
        return null;
    }
}
