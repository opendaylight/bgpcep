/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.singleton.api.ClusterSingletonServiceProvider;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.BGPRibRoutingPolicyFactory;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.state.BGPStateProviderRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(service = { })
@Designate(ocd = OSGiBgpDeployer.Configuration.class)
public final class OSGiBgpDeployer extends DefaultBgpDeployer {
    @ObjectClassDefinition(description = "Configuration of the OSGiBgpDeployer")
    public @interface Configuration {
        @AttributeDefinition(description = "Instance name of deployed BGP")
        String networkInstanceName() default "global-bgp";
    }

    @Activate
    public OSGiBgpDeployer(@Reference final ClusterSingletonServiceProvider provider,
                           @Reference final RpcProviderService rpcRegistry,
                           @Reference final RIBExtensionConsumerContext ribExtensionContext,
                           @Reference final BGPDispatcher bgpDispatcher,
                           @Reference final BGPRibRoutingPolicyFactory routingPolicyFactory,
                           @Reference final CodecsRegistry codecsRegistry,
                           @Reference final DOMDataBroker domDataBroker,
                           @Reference final DataBroker dataBroker,
                           @Reference final BGPTableTypeRegistryConsumer mappingService,
                           @Reference final BGPStateProviderRegistry stateProviderRegistry,
                           final Configuration configuration) {
        super(configuration.networkInstanceName(), provider, rpcRegistry, ribExtensionContext, bgpDispatcher,
                routingPolicyFactory, codecsRegistry, domDataBroker, dataBroker, mappingService, stateProviderRegistry);
        init();
    }

    @Override
    @Deactivate
    public void close() {
        super.close();
    }
}
