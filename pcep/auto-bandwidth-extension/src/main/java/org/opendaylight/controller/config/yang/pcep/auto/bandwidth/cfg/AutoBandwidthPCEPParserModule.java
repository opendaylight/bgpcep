/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.pcep.auto.bandwidth.cfg;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.protocol.pcep.auto.bandwidth.extension.Activator;

public class AutoBandwidthPCEPParserModule extends AbstractAutoBandwidthPCEPParserModule {

    public AutoBandwidthPCEPParserModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public AutoBandwidthPCEPParserModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver, final AutoBandwidthPCEPParserModule oldModule, final AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public AutoCloseable createInstance() {
        return new Activator(getBandwidthUsageObjectType());
    }

}
