/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPTreatAsWithdrawException;

/**
 * Enumeration of possible treatments an UPDATE message and attributes can get based on the configuration of a peer.
 *
 * @author Robert Varga
 */
@NonNullByDefault
public enum RevisedErrorHandling {
    /**
     * Do not use RFC7606 Revised Error Handling.
     */
    NONE {
        @Override
        public BGPDocumentedException throwError(final BGPTreatAsWithdrawException exception)
                throws BGPDocumentedException {
            throw exception.toDocumentedException();
        }
    },
    /**
     * Use RFC7606 Revised Error Handling, the peer is an internal neighbor.
     */
    INTERNAL,
    /**
     * Use RFC7606 Revised Error Handling, the peer is an external neighbor.
     */
    EXTERNAL;

    /**
     * Determine Revised Error Handling from the contents of a {@link PeerSpecificParserConstraint}.
     *
     * @param constraint
     * @return Revised Error Handling treatment message/attributes should receive.
     */
    public static RevisedErrorHandling from(final @Nullable PeerSpecificParserConstraint constraint) {
        return constraint == null ? NONE : constraint.getPeerConstraint(RevisedErrorHandlingSupport.class)
                .map(support -> support.isExternalPeer() ? EXTERNAL : INTERNAL).orElse(NONE);
    }

    /**
     * Convert or throw specified exception. This is useful in situations where we want either treat-as-withdraw
     * (when RFC7606 is in effect) or return an error and tear down the session (when RFC7606 is not in effect).
     *
     * @param exception Source exception
     * @return An equivalent {@link BGPDocumentedException} if Revised Error Handling is inactive
     * @throws BGPTreatAsWithdrawException if Revised Error Handling is in effect
     */
    public BGPDocumentedException throwError(final BGPTreatAsWithdrawException exception)
            throws BGPDocumentedException, BGPTreatAsWithdrawException {
        throw exception;
    }
}
