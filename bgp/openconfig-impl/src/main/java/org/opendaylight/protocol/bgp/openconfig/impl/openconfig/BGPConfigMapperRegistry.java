/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.openconfig;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenConfigProvider;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPOpenconfigMapper;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfiguration;

final class BGPConfigMapperRegistry implements BGPOpenConfigProvider {

    @SuppressWarnings("rawtypes")
    private final Map<Class<? extends InstanceConfiguration>, AbstractBGPOpenConfigMapper> configMappers = new HashMap<>();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void registerOpenConfigMapper(final AbstractBGPOpenConfigMapper openConfigMapper) {
        configMappers.put(openConfigMapper.getInstanceConfigurationType(), openConfigMapper);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends InstanceConfiguration> BGPOpenconfigMapper<T> getOpenConfigMapper(final Class<T> clazz) {
        return configMappers.get(clazz);
    }

}
