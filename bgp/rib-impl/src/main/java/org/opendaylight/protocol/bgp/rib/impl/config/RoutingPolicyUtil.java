/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.DefaultPolicyType.ACCEPTROUTE;

import java.util.Collections;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.apply.policy.group.ApplyPolicy;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.apply.policy.group.apply.policy.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.apply.policy.group.apply.policy.ConfigBuilder;

public final class RoutingPolicyUtil {
    private static final Config DEFAULT_POLICY = new ConfigBuilder().setDefaultImportPolicy(ACCEPTROUTE)
            .setDefaultExportPolicy(ACCEPTROUTE).setImportPolicy(Collections.emptyList())
            .setExportPolicy(Collections.emptyList()).build();

    private RoutingPolicyUtil() {
        // Hidden on purpose
    }

    public static Config getApplyPolicy(final ApplyPolicy applyPolicy) {
        if (applyPolicy == null) {
            return DEFAULT_POLICY;
        }
        return applyPolicy.getConfig();
    }
}
