/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bgpcep.data.change.counter;

import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.data.change.counter.TopologyDataChangeCounter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DataChangeCounterImplModule extends org.opendaylight.controller.config.yang.bgpcep.data.change.counter.AbstractDataChangeCounterImplModule {

    public DataChangeCounterImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DataChangeCounterImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.controller.config.yang.bgpcep.data.change.counter.DataChangeCounterImplModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        JmxAttributeValidationException.checkNotNull(getTopologyName(), "value is not set.", topologyNameJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final TopologyDataChangeCounter counter = new TopologyDataChangeCounter(getDataProviderDependency());
        final InstanceIdentifier<Topology> topoIId = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId(getTopologyName()))).toInstance();
        final ListenerRegistration<DataChangeListener> registration = getDataProviderDependency().registerDataChangeListener(
                LogicalDatastoreType.OPERATIONAL, topoIId, counter, DataBroker.DataChangeScope.SUBTREE);
        return new DataChangeCounterCloseable(counter, registration);
    }

    private final class DataChangeCounterCloseable implements AutoCloseable {

        private final TopologyDataChangeCounter inner;
        private final ListenerRegistration<DataChangeListener> registration;

        public DataChangeCounterCloseable(TopologyDataChangeCounter inner,
                ListenerRegistration<DataChangeListener> registration) {
            this.inner = inner;
            this.registration = registration;
        }

        @Override
        public void close() throws Exception {
            this.registration.close();
            this.inner.close();
        }
    }

}
