/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.rib.impl.config;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.protocol.bgp.openconfig.routing.policy.spi.BGPRibRoutingPolicyFactory;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.CodecsRegistry;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Singleton
@Component(immediate = true)
@Designate(ocd = OSGiBgpDeployer.Configuration.class)
public class OSGiBgpDeployer extends DefaultBgpDeployer {

    @ObjectClassDefinition(description = "Configuration of the OSGiBgpDeployer")
    public @interface Configuration {
        @AttributeDefinition(description = "Instance name of deployed BGP")
        String networkInstanceName() default "global-bgp";
    }

    @Inject
    @Activate
    public OSGiBgpDeployer(final Configuration networkInstanceNameConf,
                           @Reference final ClusterSingletonServiceProvider provider,
                           @Reference final RpcProviderService rpcRegistry,
                           @Reference final RIBExtensionConsumerContext ribExtensionContext,
                           @Reference final BGPDispatcher bgpDispatcher,
                           @Reference final BGPRibRoutingPolicyFactory routingPolicyFactory,
                           @Reference final CodecsRegistry codecsRegistry,
                           @Reference final DOMDataBroker domDataBroker,
                           @Reference final DataBroker dataBroker,
                           @Reference final BGPTableTypeRegistryConsumer mappingService) {
        super(networkInstanceNameConf.networkInstanceName(), provider, rpcRegistry, ribExtensionContext, bgpDispatcher,
                routingPolicyFactory, codecsRegistry, domDataBroker, dataBroker, mappingService);
        init();
    }

    @Override
    @PreDestroy
    @Deactivate
    public void close() {
        super.close();
    }

}
