/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import com.google.common.base.MoreObjects;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.protocol.bgp.parser.spi.RevisedErrorHandlingSupport;

@NonNullByDefault
public final class RevisedErrorHandlingSupportImpl implements RevisedErrorHandlingSupport {
    private static final RevisedErrorHandlingSupportImpl EXTERNAL_PEER = new RevisedErrorHandlingSupportImpl(true);
    private static final RevisedErrorHandlingSupportImpl INTERNAL_PEER = new RevisedErrorHandlingSupportImpl(false);

    private final boolean externalPeer;

    private RevisedErrorHandlingSupportImpl(final boolean externalPeer) {
        this.externalPeer = externalPeer;
    }

    public static RevisedErrorHandlingSupportImpl forInternalPeer() {
        return INTERNAL_PEER;
    }

    public static RevisedErrorHandlingSupportImpl forExternalPeer() {
        return EXTERNAL_PEER;
    }

    @Override
    public boolean isExternalPeer() {
        return externalPeer;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("externalPeer", externalPeer).toString();
    }
}
