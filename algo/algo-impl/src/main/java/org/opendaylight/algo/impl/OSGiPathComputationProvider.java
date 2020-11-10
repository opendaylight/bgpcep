/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.algo.impl;

import static com.google.common.base.Verify.verifyNotNull;

import org.opendaylight.algo.PathComputationAlgorithm;
import org.opendaylight.algo.PathComputationProvider;
import org.opendaylight.graph.ConnectedGraph;
import org.opendaylight.graph.ConnectedGraphProvider;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.path.computation.rev200120.AlgorithmType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true)
// FIXME: merge this with PathComputationServer once we have OSGi R7
public final class OSGiPathComputationProvider implements PathComputationProvider {
    @Reference
    RpcProviderService rpcProviderService;
    @Reference
    ConnectedGraphProvider graphProvider;

    private PathComputationServer delegate = null;

    @Override
    public PathComputationAlgorithm getPathComputationAlgorithm(final ConnectedGraph cgraph,
            final AlgorithmType algorithmType) {
        return verifyNotNull(delegate).getPathComputationAlgorithm(cgraph, algorithmType);
    }

    @Activate
    void activate() {
        final PathComputationServer local = new PathComputationServer(rpcProviderService, graphProvider);
        local.init();
        delegate = local;
    }

    @Deactivate
    void deactivate() {
        delegate.close();
        delegate = null;
    }
}
