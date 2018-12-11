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
import org.opendaylight.protocol.bgp.parser.BGPError;
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
        public BGPDocumentedException reportError(final BGPError error, final @Nullable Exception cause,
                final String format, final Object... args) throws BGPDocumentedException {
            throw new BGPDocumentedException(String.format(format, args), error, cause);
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
     * @param constraint Peer-specific constraint
     * @return Revised Error Handling treatment message/attributes should receive.
     */
    public static RevisedErrorHandling from(final @Nullable PeerSpecificParserConstraint constraint) {
        return constraint == null ? NONE : constraint.getPeerConstraint(RevisedErrorHandlingSupport.class)
                .map(support -> support.isExternalPeer() ? EXTERNAL : INTERNAL).orElse(NONE);
    }

    /**
     * Report a failure to parse an attribute resulting either in treat-as-withdraw if RFC7606 is in effect, or
     * connection teardown if it is not.
     *
     * @param error {@link BGPError} to report in case of a session teardown
     * @param cause Parsing failure cause
     * @param format Message format string
     * @param args Message format arguments
     * @return This method does not return
     * @throws BGPTreatAsWithdrawException if Revised Error Handling is in effect
     * @throws BGPDocumentedException if Revised Error Handling is in not effect
     */
    public BGPDocumentedException reportError(final BGPError error, final @Nullable Exception cause,
            final String format, final Object... args) throws BGPDocumentedException, BGPTreatAsWithdrawException {
        throw new BGPTreatAsWithdrawException(error, cause, format, args);
    }

    /**
     * Report a failure to parse an attribute resulting either in treat-as-withdraw if RFC7606 is in effect, or
     * connection teardown if it is not.
     *
     * @param error {@link BGPError} to report in case of a session teardown
     * @param format Message format string
     * @param args Message format arguments
     * @return This method does not return
     * @throws BGPTreatAsWithdrawException if Revised Error Handling is in effect
     * @throws BGPDocumentedException if Revised Error Handling is in not effect
     */
    public BGPDocumentedException reportError(final BGPError error, final String format, final Object... args)
            throws BGPDocumentedException, BGPTreatAsWithdrawException {
        throw reportError(error, (Exception) null, format, args);
    }

    /**
     * Report a failure to parse an attribute resulting either in treat-as-withdraw if RFC7606 is in effect, or
     * connection teardown if it is not.
     *
     * @param error {@link BGPError} to report in case of a session teardown
     * @param format Message format string
     * @param args Message format arguments
     * @return This method does not return
     * @throws BGPTreatAsWithdrawException if Revised Error Handling is in effect
     * @throws BGPDocumentedException if Revised Error Handling is in not effect
     */
    public BGPDocumentedException reportError(final BGPError error, final byte[] data, final String format,
            final Object... args) throws BGPDocumentedException, BGPTreatAsWithdrawException {
        throw new BGPTreatAsWithdrawException(error, data, format, args);
    }
}
