/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import org.opendaylight.protocol.bgp.mode.api.BestPath;
import org.opendaylight.protocol.bgp.mode.api.BestPathState;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public abstract class AbstractBestPath implements BestPath {
    protected final BestPathState state;

    protected AbstractBestPath(final BestPathState state) {
        this.state = requireNonNull(state);
    }

    protected abstract MoreObjects.ToStringHelper addToStringAttributes(MoreObjects.ToStringHelper toStringHelper);

    @VisibleForTesting
    public final BestPathState getState() {
        return this.state;
    }

    @Override
    public final ContainerNode getAttributes() {
        return this.state.getAttributes();
    }

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(this)).toString();
    }
}
