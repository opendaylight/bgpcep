/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.spi.pojo;

import com.google.common.base.Preconditions;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfiguration;
import org.opendaylight.protocol.bgp.openconfig.spi.InstanceConfigurationIdentifier;

abstract class AbstractInstanceConfiguration implements InstanceConfiguration {

    private final InstanceConfigurationIdentifier identifier;

    protected AbstractInstanceConfiguration(final InstanceConfigurationIdentifier identifier) {
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    @Override
    public InstanceConfigurationIdentifier getIdentifier() {
        return this.identifier;
    }
}
