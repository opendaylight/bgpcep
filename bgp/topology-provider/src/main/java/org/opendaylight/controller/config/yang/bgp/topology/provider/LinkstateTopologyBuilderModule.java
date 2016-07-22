/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * Generated file

 * Generated from: yang module name: config-bgp-topology-provider  yang module local name: bgp-linkstate-topology
 * Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
 * Generated at: Tue Nov 19 15:22:41 CET 2013
 *
 * Do not modify this file unless it is present under src/main directory
 */
package org.opendaylight.controller.config.yang.bgp.topology.provider;

import java.util.concurrent.ExecutionException;
import org.opendaylight.bgpcep.bgp.topology.provider.LinkstateTopologyBuilder;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public final class LinkstateTopologyBuilderModule extends org.opendaylight.controller.config.yang.bgp.topology.provider.AbstractLinkstateTopologyBuilderModule {
    private static final Logger LOG = LoggerFactory.getLogger(LinkstateTopologyBuilderModule.class);

    public LinkstateTopologyBuilderModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
        final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public LinkstateTopologyBuilderModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
        final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
        final LinkstateTopologyBuilderModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void validate() {
        super.validate();
        JmxAttributeValidationException.checkNotNull(getTopologyId(), "is not set.", topologyIdJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new LinkstateTopologyBuilder(getDataProviderDependency(), getLocalRibDependency(), getTopologyId());
    }
}
