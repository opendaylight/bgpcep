/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl;

import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigHolder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.Global;

public class BGPConfigStateHolders {

    private final BGPConfigHolder<String, Global> globalConfigHolder = new BGPConfigHolderImpl<>();

    public BGPConfigHolder<String, Global> getGlobalConfigHolder() {
        return globalConfigHolder;
    }

}
