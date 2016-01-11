/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.pcep.auto.bandwidth.cfg;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.protocol.pcep.auto.bandwidth.extension.Activator;

public class AutoBandwidthPCEPParserModule extends AbstractAutoBandwidthPCEPParserModule {

    private static final int BW_TYPE_RANGE_LOW = 3;
    private static final int BW_TYPE_RANGE_UP = 15;

    public AutoBandwidthPCEPParserModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public AutoBandwidthPCEPParserModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver, final AutoBandwidthPCEPParserModule oldModule, final AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        //values 3-15 are unassigned - http://www.iana.org/assignments/pcep/pcep.xhtml#pcep-objects
        JmxAttributeValidationException.checkCondition(getBandwidthUsageObjectType() >= BW_TYPE_RANGE_LOW &&
                getBandwidthUsageObjectType() <= BW_TYPE_RANGE_UP, "out of range 3..15", bandwidthUsageObjectTypeJmxAttribute);
    }

    @Override
    public AutoCloseable createInstance() {
        return new Activator(getBandwidthUsageObjectType());
    }

}
