/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

/**
 * Peer constraint supporting RFC7606 Revised Error Handling. Presence of this class in
 * {@link PeerSpecificParserConstraint} indicates revised error handling procedures should be followed when parsing
 * messages and attributes.
 *
 * @author Robert Varga
 */
public interface RevisedErrorHandlingSupport extends PeerConstraint {
    /**
     * Return true if the peer is external.
     *
     * @return True if the peer is external, false if the peer is internal.
     */
    boolean isExternalPeer();
}
