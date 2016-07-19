/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl.config;

import static org.opendaylight.protocol.bgp.rib.impl.config.OpenConfigMappingUtil.getRibInstanceName;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.protocol.bgp.rib.impl.spi.BgpDeployer;
import org.opendaylight.protocol.bgp.rib.impl.spi.InstanceType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.bgp.Global;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.Protocols;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.openconfig.extensions.rev160614.Protocol1;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BgpRibDeployer implements AutoCloseable, DataTreeChangeListener<Global> {

    private static final Logger LOG = LoggerFactory.getLogger(BgpRibDeployer.class);

    private final BgpDeployer bgpDeployer;
    private final ListenerRegistration<?> registration;
    private final Map<InstanceIdentifier<Global>, RibImpl> ribs = new HashMap<>();

    public BgpRibDeployer(final BgpDeployer bgpDeployer) {
        this.bgpDeployer = bgpDeployer;
        this.registration = bgpDeployer.registerDataTreeChangeListener(this, bgpDeployer.getInstanceIdentifier().child(Protocols.class)
                .child(Protocol.class)
                .augmentation(Protocol1.class)
                .child(Bgp.class).child(Global.class));
        LOG.info("BGP RIB Deployer started.");
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<Global>> changes) {
        for (final DataTreeModification<Global> dataTreeModification : changes) {
            final InstanceIdentifier<Global> rootIdentifier = dataTreeModification.getRootPath().getRootIdentifier();
            final DataObjectModification<Global> rootNode = dataTreeModification.getRootNode();
            switch (rootNode.getModificationType()) {
                case DELETE:
                    onGlobalRemoved(rootIdentifier);
                    break;
                case SUBTREE_MODIFIED:
                    onGlobalModified(rootIdentifier, rootNode.getDataAfter());
                    break;
                case WRITE:
                    onGlobalCreated(rootIdentifier, rootNode.getDataAfter());
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void close() {
        this.registration.close();
        this.ribs.values().forEach(ribImpl -> ribImpl.close());
        this.ribs.clear();
        LOG.info("BGP RIB Deployer stopped.");
    }

    private void onGlobalModified(final InstanceIdentifier<Global> rootIdentifier, final Global global) {
        //restart rib instance with a new configuration
        LOG.debug("Modifing RIB instance with configuration: {}", global);
        final RibImpl ribImpl = this.ribs.get(rootIdentifier);
        ribImpl.close();
        initiateRibInstance(rootIdentifier, global, ribImpl);
        LOG.debug("RIB instance modified {}", ribImpl);
    }

    private void onGlobalCreated(final InstanceIdentifier<Global> rootIdentifier, final Global global) {
        //create, start and register rib instance
        LOG.debug("Creating RIB instance with configuration: {}", global);
        final RibImpl ribImpl = (RibImpl) this.bgpDeployer.getComponentInstance(InstanceType.RIB);
        initiateRibInstance(rootIdentifier, global, ribImpl);
        this.ribs.put(rootIdentifier, ribImpl);
        LOG.debug("RIB instance created {}", ribImpl);
    }

    private void onGlobalRemoved(final InstanceIdentifier<Global> rootIdentifier) {
        //destroy rib instance
        LOG.debug("Removing RIB instance: {}", rootIdentifier);
        final RibImpl ribImpl = this.ribs.remove(rootIdentifier);
        ribImpl.close();
        LOG.debug("RIB instance created {}", ribImpl);
    }

    private void registerRibInstance(final RibImpl ribImpl, final String ribInstanceName) {
        final ServiceRegistration<RibImpl> serviceRegistration = this.bgpDeployer.registerService(InstanceType.RIB, ribImpl, ribInstanceName);
        ribImpl.setServiceRegistration(serviceRegistration);
    }

    private void initiateRibInstance(final InstanceIdentifier<Global> rootIdentifier, final Global global,
            final RibImpl ribImpl) {
        final String ribInstanceName = getRibInstanceName(rootIdentifier);
        ribImpl.start(global, ribInstanceName, this.bgpDeployer.getMappingService());
        registerRibInstance(ribImpl, ribInstanceName);
    }

}
