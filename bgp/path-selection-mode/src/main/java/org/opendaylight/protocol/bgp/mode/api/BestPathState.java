/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.api;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yangtools.yang.common.Uint32;

public interface BestPathState {
    @Nullable Uint32 getLocalPref();

    long getMultiExitDisc();

    @Nullable BgpOrigin getOrigin();

    long getPeerAs();

    int getAsPathLength();

    /**
     * Return true if this route is depreferenced, for example through LLGR_STALE community.
     *
     * @return True if this route is depreferenced, false otherwise.
     */
    boolean isDepreferenced();

    @NonNull Attributes getAttributes();
}