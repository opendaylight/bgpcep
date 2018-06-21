/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.impl;

import com.google.common.collect.Lists;
import java.util.List;
import org.opendaylight.protocol.bgp.openconfig.spi.AbstractBGPTableTypeRegistryProviderActivator;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.LinkstateAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.LinkstateSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev171207.LINKSTATE;
import org.opendaylight.yangtools.concepts.AbstractRegistration;

public final class TableTypeActivator extends AbstractBGPTableTypeRegistryProviderActivator {

    @Override
    protected List<AbstractRegistration> startBGPTableTypeRegistryProviderImpl(final BGPTableTypeRegistryProvider provider) {
        return Lists.newArrayList(provider.registerBGPTableType(LinkstateAddressFamily.class, LinkstateSubsequentAddressFamily.class, LINKSTATE.class));
    }

}
