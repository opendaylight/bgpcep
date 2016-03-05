/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.mode.api;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;

public interface BestPathState {
    Long getLocalPref();

    Long getMultiExitDisc();

    BgpOrigin getOrigin();

    Long getPeerAs();

    int getAsPathLength();

    ContainerNode getAttributes();
}