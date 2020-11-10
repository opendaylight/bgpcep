/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.graph.impl;

import org.opendaylight.graph.ConnectedGraphProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true)
// FIXME: merge this with GraphListener once we have constructor injection
public final class OSGiGraphListener {
    @Reference
    DataBroker dataBroker;
    @Reference
    ConnectedGraphProvider graphProvider;

    private GraphListener delegate;

    @Activate
    void activate() {
        GraphListener local = new GraphListener(dataBroker, graphProvider);
        local.init();
        delegate = local;
    }

    @Deactivate
    void deactivate() {
        delegate.close();
        delegate = null;
    }
}
