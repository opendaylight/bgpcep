/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.mode.impl.base;

import org.opendaylight.protocol.bgp.mode.api.PathSelectionMode;

public final class BasePathSelectionModeFactory {

    private BasePathSelectionModeFactory() {
        throw new UnsupportedOperationException();
    }

    public static PathSelectionMode createBestPathSelectionStrategy() {
        return new BasePathSelection();
    }
}
