/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi;

/**
 * Holds BGP OpenConfig configuration components mappers.
 */
public interface BGPOpenConfigProvider {

    /**
     * Return OpenConfig mapper for particular BGP InstanceConfiguration holder
     * @param clazz The class type of a InstanceConfiguration to which the BGPOpenconfigMapper
     * is binded.
     * @return BGPOpenconfigMapper
     */
    <T extends InstanceConfiguration> BGPOpenconfigMapper<T> getOpenConfigMapper(Class<T> clazz);

}
