/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.ext.community.sets;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.ext.community.sets.ExtCommunitySet.ExtCommunityMember;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.BgpCommunityRegexpType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.BgpExtCommunityType;

/**
 * The purpose of generated class in src/main/java for Union types is to create new instances of unions from a string representation.
 * In some cases it is very difficult to automate it since there can be unions such as (uint32 - uint16), or (string - uint32).
 *
 * The reason behind putting it under src/main/java is:
 * This class is generated in form of a stub and needs to be finished by the user. This class is generated only once to prevent
 * loss of user code.
 *
 */
public class ExtCommunitySetExtCommunityMemberBuilder {

    private static final Pattern EXT_COMM_PATTERN = Pattern.compile("^route\\-origin:(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5]):(6[0-5][0-5][0-3][0-5]|[1-5][0-9]{4}|[1-9][0-9]{1,4}|[0-9])$");

    public static ExtCommunityMember getDefaultInstance(final java.lang.String defaultValue) {
        final Matcher matcher = EXT_COMM_PATTERN.matcher(defaultValue);

        if (matcher.matches()) {
            final BgpExtCommunityType bgpExtCommunityType = new BgpExtCommunityType(defaultValue);
            return new ExtCommunityMember(bgpExtCommunityType);
        }
        return new ExtCommunityMember(new BgpCommunityRegexpType(defaultValue));
    }

}
