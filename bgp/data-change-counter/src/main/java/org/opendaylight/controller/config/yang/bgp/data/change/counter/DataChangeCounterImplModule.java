/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bgp.data.change.counter;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.data.change.counter.LinkstateTopologyDataChangeCounter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class DataChangeCounterImplModule extends
        org.opendaylight.controller.config.yang.bgp.data.change.counter.AbstractDataChangeCounterImplModule {

    private static final String TOPO_ID = "example-linkstate-topology";
    private static final InstanceIdentifier<Topology> TOPO_IID = InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId(TOPO_ID))).toInstance();

    private BundleContext bundleCtx;

    public DataChangeCounterImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DataChangeCounterImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.controller.config.yang.bgp.data.change.counter.DataChangeCounterImplModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        Preconditions.checkState(getDataBroker() != null, "DataBroker service was not found.");
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final DataBroker dataBroker = getDataBroker();
        final LinkstateTopologyDataChangeCounter counter = new LinkstateTopologyDataChangeCounter(dataBroker);
        final ListenerRegistration<DataChangeListener> registration = dataBroker.registerDataChangeListener(
                LogicalDatastoreType.OPERATIONAL, TOPO_IID, counter, DataBroker.DataChangeScope.SUBTREE);
        return new DataChangeCounterCloseable(counter, registration);
    }

    protected void setBundleContext(final BundleContext bundleCtx) {
        this.bundleCtx = bundleCtx;
    }

    private DataBroker getDataBroker() {
        final ServiceReference<DataBroker> serviceRef = this.bundleCtx.getServiceReference(DataBroker.class);
        if (serviceRef != null) {
            return this.bundleCtx.getService(serviceRef);
        }
        return null;
    }

    private final class DataChangeCounterCloseable implements AutoCloseable {

        private final LinkstateTopologyDataChangeCounter inner;
        private final ListenerRegistration<DataChangeListener> registration;

        public DataChangeCounterCloseable(LinkstateTopologyDataChangeCounter inner,
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
