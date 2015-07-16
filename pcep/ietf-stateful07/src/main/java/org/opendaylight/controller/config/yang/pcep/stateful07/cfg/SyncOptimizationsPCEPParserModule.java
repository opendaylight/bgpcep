/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.pcep.stateful07.cfg;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.protocol.pcep.sync.optimizations.SyncOptimizationsActivator;

public class SyncOptimizationsPCEPParserModule extends AbstractSyncOptimizationsPCEPParserModule {

    public SyncOptimizationsPCEPParserModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public SyncOptimizationsPCEPParserModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver,
        final SyncOptimizationsPCEPParserModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected AutoCloseable createInstance() {
        return new SyncOptimizationsActivator();
    }

}
