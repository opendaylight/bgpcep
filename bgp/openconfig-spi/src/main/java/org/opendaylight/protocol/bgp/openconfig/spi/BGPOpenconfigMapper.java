/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi;

import javax.annotation.Nonnull;

/**
 * The BGPOpenconfigMapper provides operations for adding/removing of BGP OpenConfig
 * components mapped to BGP configuration modules
 *
 * @param <T> The type of a BGP module configuration holder to which the mapper is binded
 */
public interface BGPOpenconfigMapper<T extends InstanceConfiguration> {

    /**
     * Add a new or replace an exiting configuration. The write operation is done in
     * non-blocking fashion.
     * @param instanceConfiguration An input configuration is mapped to OpenConfig API
     * @throws NullPointerException when the instanceConfiguration is null
     */
    void writeConfiguration(@Nonnull T instanceConfiguration);

    /**
     * Remove an existing configuration.
     * @param instanceConfiguration An input configuration is mapped to OpenConfig API
     * @throws NullPointerException when instanceConfiguration is null
     */
    void removeConfiguration(@Nonnull T instanceConfiguration);

}
