/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev150515.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.set.ext.community.set.ext.community.method;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev150515.routing.policy.policy.definitions.policy.definition.statements.statement.actions.bgp.actions.set.ext.community.set.ext.community.method.Inline.Communities;

public class InlineCommunitiesBuilderTest {

    @Test
    public void testValid() {
        final Communities communities = InlineCommunitiesBuilder.getDefaultInstance("123:123");
        Assert.assertEquals("123:123", communities.getBgpExtCommunityType().getValue());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalid() {
        InlineCommunitiesBuilder.getDefaultInstance("abc");
    }

}
