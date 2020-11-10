/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.graph.impl;

import static com.google.common.base.Verify.verifyNotNull;

import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedGraphProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.Graph;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.Graph.DomainScope;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.graph.rev191125.graph.topology.GraphKey;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true)
// FIXME: merge this with ConnectedGraphServer once we have constructor injection
public final class OSGiConnectedGraphProvider implements ConnectedGraphProvider {
    @Reference
    DataBroker dataBroker;

    private ConnectedGraphServer delegate;

    @Override
    public Graph getGraph(final String name) {
        return delegate().getGraph(name);
    }

    @Override
    public Graph getGraph(final GraphKey key) {
        return delegate().getGraph(key);
    }

    @Override
    public ConnectedGraph getConnectedGraph(final String name) {
        return delegate().getConnectedGraph(name);
    }

    @Override
    public ConnectedGraph getConnectedGraph(final GraphKey key) {
        return delegate().getConnectedGraph(key);
    }

    @Override
    public List<ConnectedGraph> getConnectedGraphs() {
        return delegate().getConnectedGraphs();
    }

    @Override
    public ConnectedGraph createConnectedGraph(final String name, final DomainScope scope) {
        return delegate().createConnectedGraph(name, scope);
    }

    @Override
    public ConnectedGraph addGraph(final Graph graph) {
        return delegate().addGraph(graph);
    }

    @Override
    public void deleteGraph(final GraphKey key) {
        delegate().deleteGraph(key);
    }

    @Activate
    void activate() {
        final ConnectedGraphServer local = new ConnectedGraphServer(dataBroker);
        local.init();
        delegate = local;
    }

    @Activate
    void deactivate() {
        delegate.close();
        delegate = null;
    }

    private @NonNull ConnectedGraphServer delegate() {
        return verifyNotNull(delegate);
    }
}
