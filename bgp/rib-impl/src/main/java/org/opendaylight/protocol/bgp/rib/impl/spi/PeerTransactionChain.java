/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.spi;

import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;

/**
 * Provides access to unique DOMTransactionChain per Peer.
 */
public interface PeerTransactionChain {
    /**
     * Returns Peer DOMTransactionChain.
     *
     * @return DOMTransactionChain
     */
    DOMTransactionChain getDomChain();
}
