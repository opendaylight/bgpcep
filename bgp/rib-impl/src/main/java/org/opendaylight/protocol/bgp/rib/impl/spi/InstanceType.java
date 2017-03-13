/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.spi;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerRuntimeMXBean;
import org.opendaylight.protocol.bgp.rib.RibReference;

public enum InstanceType {

    RIB("ribImpl", Lists.newArrayList(RIB.class, RibReference.class)),

    PEER("bgpPeer", Collections.singletonList(BGPPeerRuntimeMXBean.class)),

    APP_PEER("appPeer", Collections.emptyList());

    private final String beanName;
    private final String[] services;

    InstanceType(final String beanName, final List<Class<?>> services) {
        this.beanName = beanName;
        this.services = new String[services.size()];
        services.stream().map(Class::getName).collect(Collectors.toList()).toArray(this.services);
    }

    public String getBeanName() {
        return this.beanName;
    }

    public String[] getServices() {
        return this.services;
    }

}
