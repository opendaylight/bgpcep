/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bmp.impl.config;

import static java.util.Objects.requireNonNull;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTree;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeFactory;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class BmpDeployerDependencies {
    private final DataBroker dataBroker;
    private final RIBExtensionConsumerContext extensions;
    private final BindingCodecTree tree;
    private final DOMDataBroker domDataBroker;
    private final ClusterSingletonServiceProvider singletonProvider;

    public BmpDeployerDependencies(final DataBroker dataBroker, final DOMDataBroker domDataBroker,
    final RIBExtensionConsumerContext extensions, final BindingCodecTreeFactory codecTreeFactory,
    final SchemaContext schemaContext, final ClusterSingletonServiceProvider singletonProvider) {
        this.dataBroker = requireNonNull(dataBroker);
        this.domDataBroker = requireNonNull(domDataBroker);
        this.extensions = requireNonNull(extensions);
        this.tree = requireNonNull(codecTreeFactory).create(schemaContext);
        this.singletonProvider = requireNonNull(singletonProvider);
    }

    public DataBroker getDataBroker() {
        return this.dataBroker;
    }

    public RIBExtensionConsumerContext getExtensions() {
        return this.extensions;
    }

    public BindingCodecTree getTree() {
        return this.tree;
    }

    public DOMDataBroker getDomDataBroker() {
        return this.domDataBroker;
    }

    public ClusterSingletonServiceProvider getClusterSingletonProvider() {
        return this.singletonProvider;
    }
}
