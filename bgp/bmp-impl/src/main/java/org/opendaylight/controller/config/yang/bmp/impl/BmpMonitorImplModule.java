/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bmp.impl;

import com.google.common.base.Optional;
import com.google.common.net.InetAddresses;
import io.netty.util.internal.PlatformDependent;
import java.net.InetSocketAddress;
import java.security.AccessControlException;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.protocol.bmp.impl.app.BmpMonitoringStationImpl;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.MonitorId;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class BmpMonitorImplModule extends org.opendaylight.controller.config.yang.bmp.impl.AbstractBmpMonitorImplModule {

    private static final int PRIVILEGED_PORTS = 1024;

    private BundleContext bundleContext;

    public BmpMonitorImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BmpMonitorImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.bmp.impl.BmpMonitorImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        JmxAttributeValidationException.checkNotNull(getBindingPort(), bindingPortJmxAttribute);
        // check if unix root user
        if (!PlatformDependent.isWindows() && !PlatformDependent.isRoot() && getBindingPort().getValue() < PRIVILEGED_PORTS) {
            throw new AccessControlException("Unable to bind port " + getBindingPort().getValue() + " while running as non-root user.");
        }
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        try {
            return BmpMonitoringStationImpl.createBmpMonitorInstance(getExtensionsDependency(), getBmpDispatcherDependency(),
                    getDomDataProviderDependency(), new MonitorId(getIdentifier().getInstanceName()),
                    getAddress(getBindingAddress(), getBindingPort()),
                    Optional.<KeyMapping>absent(), getCodecTreeFactoryDependency(), getSchemaProvider());
        } catch(final InterruptedException e) {
            throw new IllegalStateException("Failed to istantiate BMP application.", e);
        }
    }

    private SchemaContext getSchemaProvider() {
        if (getDomDataProviderDependency() instanceof SchemaContextProvider) {
            return ((SchemaContextProvider) getDomDataProviderDependency()).getSchemaContext();
        }
        final ServiceReference<SchemaService> serviceRef = this.bundleContext.getServiceReference(SchemaService.class);
        return this.bundleContext.getService(serviceRef).getGlobalContext();
    }

    private static InetSocketAddress getAddress(final IpAddress ipAddress, final PortNumber port) {
        final String ipString;
        if (ipAddress.getIpv4Address() != null) {
            ipString = ipAddress.getIpv4Address().getValue();
        } else {
            ipString = ipAddress.getIpv6Address().getValue();
        }

        return new InetSocketAddress(InetAddresses.forString(ipString), port.getValue());
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
