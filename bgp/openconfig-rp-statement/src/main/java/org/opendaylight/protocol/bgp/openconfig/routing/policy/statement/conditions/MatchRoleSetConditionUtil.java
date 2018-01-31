/*
 * Copyright (c) 2018 AT&T Intellectual Property. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.routing.policy.statement.conditions;

import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.MatchSetOptionsRestrictedType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp._default.policy.rev180109.match.role.set.condition.MatchRoleSet;

public final class MatchRoleSetConditionUtil {
    private MatchRoleSetConditionUtil() {
        throw new UnsupportedOperationException();
    }

    public static boolean matchRoleCondition(final MatchRoleSet neighCond, final PeerRole role) {
        if (neighCond.getMatchSetOptions().equals(MatchSetOptionsRestrictedType.ANY)) {
            return neighCond.getRole().contains(role);
        }
        //INVERT Case
        return !neighCond.getRole().contains(role);
    }
}
