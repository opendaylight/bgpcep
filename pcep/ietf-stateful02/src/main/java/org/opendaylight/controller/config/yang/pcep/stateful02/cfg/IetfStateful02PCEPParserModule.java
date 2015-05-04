/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.pcep.stateful02.cfg;

import org.opendaylight.protocol.pcep.ietf.stateful02.StatefulActivator;

/**
*
*/
@Deprecated
public final class IetfStateful02PCEPParserModule extends
        org.opendaylight.controller.config.yang.pcep.stateful02.cfg.AbstractIetfStateful02PCEPParserModule {

    public IetfStateful02PCEPParserModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public IetfStateful02PCEPParserModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final IetfStateful02PCEPParserModule oldModule, final java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new StatefulActivator();
    }
}
