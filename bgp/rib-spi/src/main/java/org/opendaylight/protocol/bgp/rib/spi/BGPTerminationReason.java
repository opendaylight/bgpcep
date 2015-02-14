/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.base.MoreObjects;
import org.opendaylight.protocol.bgp.parser.BGPError;
import org.opendaylight.protocol.framework.TerminationReason;

public final class BGPTerminationReason implements TerminationReason {
    private final BGPError error;

    public BGPTerminationReason(final BGPError error) {
        this.error = error;
    }

    @Override
    public String getErrorMessage() {
        return error.toString();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("error", error)
                .toString();
    }
}
