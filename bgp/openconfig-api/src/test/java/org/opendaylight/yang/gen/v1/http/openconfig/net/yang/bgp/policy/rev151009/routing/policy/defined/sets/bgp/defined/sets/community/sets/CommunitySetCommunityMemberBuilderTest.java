/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.community.sets;


import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.policy.rev151009.routing.policy.defined.sets.bgp.defined.sets.community.sets.CommunitySet.CommunityMember;

public class CommunitySetCommunityMemberBuilderTest {

    @Test
    public void testStdComType() {
        final CommunityMember communityMemeber = CommunitySetCommunityMemberBuilder.getDefaultInstance("123");
        Assert.assertEquals(123L, communityMemeber.getBgpStdCommunityType().getUint32().longValue());
    }

    @Test
    public void testRegxpComType() {
        final CommunityMember communityMemeber = CommunitySetCommunityMemberBuilder.getDefaultInstance("[0-9]");
        Assert.assertEquals("[0-9]", communityMemeber.getBgpCommunityRegexpType().getValue());
    }

}
