/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * Generated file

 * Generated from: yang module name: bgp-rib-impl  yang module local name: rib-impl
 * Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
 * Generated at: Wed Nov 06 13:02:32 CET 2013
 *
 * Do not modify this file unless it is present under src/main directory
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import java.util.Hashtable;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPConfigModuleListener;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPGlobalWriter;
import org.opendaylight.protocol.bgp.rib.impl.RIBImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.osgi.framework.BundleContext;

/**
 *
 */
public final class RIBImplModule extends org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractRIBImplModule {

    private static final String IS_NOT_SET = "is not set.";
    private BundleContext bundleContext;

    public RIBImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier name,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(name, dependencyResolver);
    }

    public RIBImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier name,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final RIBImplModule oldModule,
            final java.lang.AutoCloseable oldInstance) {
        super(name, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        JmxAttributeValidationException.checkNotNull(getExtensions(), IS_NOT_SET, extensionsJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getRibId(), IS_NOT_SET, ribIdJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getLocalAs(), IS_NOT_SET, localAsJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getBgpRibId(), IS_NOT_SET, bgpRibIdJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getTcpReconnectStrategy(), IS_NOT_SET, tcpReconnectStrategyJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getSessionReconnectStrategy(), IS_NOT_SET, sessionReconnectStrategyJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getLocalTable(), IS_NOT_SET, localTableJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final AsNumber asNumber = new AsNumber(getLocalAs());
        final BGPGlobalWriter globalWriter = getGlobalWriter();
        final String instanceName = getIdentifier().getInstanceName();
        final RIBImpl rib = new RIBImpl(getRibId(), asNumber, getBgpRibId(), getClusterId(), getExtensionsDependency(),
            getBgpDispatcherDependency(), getTcpReconnectStrategyDependency(), getCodecTreeFactoryDependency(), getSessionReconnectStrategyDependency(),
            getDataProviderDependency(), getDomDataProviderDependency(), getLocalTableDependency(), classLoadingStrategy(),
            new RIBImplModuleTracker(getGlobalWriter()));
        registerSchemaContextListener(rib);
        return rib;
    }

    private GeneratedClassLoadingStrategy classLoadingStrategy() {
        return getExtensionsDependency().getClassLoadingStrategy();
    }

    private void registerSchemaContextListener(final RIBImpl rib) {
        final DOMDataBroker domBroker = getDomDataProviderDependency();
        if(domBroker instanceof SchemaService) {
            ((SchemaService) domBroker).registerSchemaContextListener(rib);
        } else {
            // FIXME:Get bundle context and register global schema service from bundle
            // context.
            bundleContext.registerService(SchemaContextListener.class, rib, new Hashtable<String,String>());
        }
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    private BGPGlobalWriter getGlobalWriter() {
        if (getOpenconfigWriterDependency() != null) {
            return getOpenconfigWriterDependency().getGlobalWriter();
        }
        return null;
    }

    protected final class RIBImplModuleTracker implements BGPConfigModuleListener {

        private final BGPGlobalWriter globalWriter;

        public RIBImplModuleTracker(final BGPGlobalWriter globalWriter) {
            this.globalWriter = globalWriter;
        }

        @Override
        public void onInstanceCreate() {
            if (globalWriter != null) {
                globalWriter.writeGlobal(RIBImplModule.this.getIdentifier().getInstanceName(), new AsNumber(RIBImplModule.this.getLocalAs()),
                        RIBImplModule.this.getBgpRibId(), RIBImplModule.this.getClusterId(), RIBImplModule.this.getLocalTableDependency());
            }
        }

        @Override
        public void onInstanceClose() {
            if (globalWriter != null) {
                globalWriter.removeGlobal(RIBImplModule.this.getIdentifier().getInstanceName());
            }
        }

    }
}
