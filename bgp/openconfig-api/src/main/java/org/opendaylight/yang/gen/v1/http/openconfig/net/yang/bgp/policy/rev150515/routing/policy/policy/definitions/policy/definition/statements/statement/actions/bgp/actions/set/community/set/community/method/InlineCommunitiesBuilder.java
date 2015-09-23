/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev150515.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.set.community.set.community.method;

import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev150515.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.set.community.set.community.method.Inline.Communities;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev150515.BgpStdCommunityType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev150515.BgpStdCommunityTypeBuilder;

/**
 * The purpose of generated class in src/main/java for Union types is to create new instances of unions from a string representation.
 * In some cases it is very difficult to automate it since there can be unions such as (uint32 - uint16), or (string - uint32).
 *
 * The reason behind putting it under src/main/java is:
 * This class is generated in form of a stub and needs to be finished by the user. This class is generated only once to prevent
 * loss of user code.
 *
 */
public class InlineCommunitiesBuilder {

    public static Communities getDefaultInstance(final java.lang.String defaultValue) {
        try {
            final BgpStdCommunityType commType = BgpStdCommunityTypeBuilder.getDefaultInstance(defaultValue);
            return new Communities(commType);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("Cannot create Communities from " + defaultValue);
        }
    }

}
