/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.spi;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import org.opendaylight.protocol.bgp.parser.BGPError;

public final class BGPTerminationReason {
    private final BGPError error;

    public BGPTerminationReason(final BGPError error) {
        this.error = error;
    }

    public String getErrorMessage() {
        return this.error.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.error);
    }

    @Override
    public boolean equals(final Object o) {
        return (o instanceof BGPTerminationReason) &&
                Objects.equals(this.error, ((BGPTerminationReason) o).error);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("error", this.error)
                .toString();
    }
}
