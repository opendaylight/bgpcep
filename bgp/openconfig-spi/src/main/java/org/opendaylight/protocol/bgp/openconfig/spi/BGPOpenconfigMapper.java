/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi;

/**
 * The BGPOpenconfigMapper provides operations for adding/removing of BGP OpenConfig
 * components mapped to BGP configuration modules
 *
 * @param <T> The type of a BGP module configuration holder
 */
public interface BGPOpenconfigMapper<T extends InstanceConfiguration> {

    void writeConfiguration(T instanceConfiguration);

    void removeConfiguration(T instanceConfiguration);

}
