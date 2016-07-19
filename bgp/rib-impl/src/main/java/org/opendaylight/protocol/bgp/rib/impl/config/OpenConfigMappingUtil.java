/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class OpenConfigMappingUtil {

    private OpenConfigMappingUtil() {
        throw new UnsupportedOperationException();
    }

    public static String getRibInstanceName(final InstanceIdentifier<?> rootIdentifier) {
        return rootIdentifier.firstKeyOf(Protocol.class).getName();
    }

}
