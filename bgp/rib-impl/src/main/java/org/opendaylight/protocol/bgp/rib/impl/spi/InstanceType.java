/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.spi;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPPeerStateConsumer;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPRIBStateConsumer;

public enum InstanceType {

    RIB("ribImpl", ImmutableList.of(RIB.class, RibReference.class, BGPRIBStateConsumer.class)),

    PEER("bgpPeer", ImmutableList.of(BGPPeerStateConsumer.class)),

    APP_PEER("appPeer", Collections.singletonList(BGPPeerStateConsumer.class));

    private final String beanName;
    private final List<String> services;

    InstanceType(final String beanName, final List<Class<?>> services) {
        this.beanName = beanName;
        this.services = ImmutableList.copyOf(services.stream().map(Class::getName).collect(Collectors.toList()));
    }

    public String getBeanName() {
        return this.beanName;
    }

    public String[] getServices() {
        return this.services.toArray(new String[0]);
    }
}
