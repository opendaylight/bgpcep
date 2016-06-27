/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.bmp.impl;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import io.netty.util.internal.PlatformDependent;
import java.security.AccessControlException;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.protocol.bmp.impl.app.BmpMonitoringStationImpl;
import org.opendaylight.protocol.util.Ipv4Util;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.MonitorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.tcpmd5.cfg.rev140427.Rfc2385Key;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BmpMonitorImplModule extends org.opendaylight.controller.config.yang.bmp.impl.AbstractBmpMonitorImplModule {

    private static final Logger LOG = LoggerFactory.getLogger(BmpMonitorImplModule.class);

    private static final int PRIVILEGED_PORTS = 1024;

    private BundleContext bundleContext;

    public BmpMonitorImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BmpMonitorImplModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.bmp.impl.BmpMonitorImplModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    private String getAddressString(final IpAddress address) {
        Preconditions.checkArgument(address.getIpv4Address() != null || address.getIpv6Address() != null, "Address %s is invalid", address);
        if (address.getIpv4Address() != null) {
            return address.getIpv4Address().getValue();
        }
        return address.getIpv6Address().getValue();
    }

    private Optional<KeyMapping> constructKeys() {
        final KeyMapping ret = new KeyMapping();
        if (getMonitoredRouter() != null) {
            for (final MonitoredRouter mr : getMonitoredRouter()) {
                if (mr.getAddress() == null) {
                    LOG.warn("Monitored router {} does not have an address skipping it", mr);
                    continue;
                }
                final Rfc2385Key rfc2385KeyPassword = mr.getPassword();
                if (rfc2385KeyPassword != null && !rfc2385KeyPassword.getValue().isEmpty()) {
                    final String s = getAddressString(mr.getAddress());
                    ret.put(InetAddresses.forString(s), rfc2385KeyPassword.getValue().getBytes(Charsets.US_ASCII));
                }
            }
        }

        return ret.isEmpty() ? Optional.<KeyMapping>absent() : Optional.<KeyMapping>of(ret);
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
                    Ipv4Util.toInetSocketAddress(getBindingAddress(), getBindingPort()),
                    constructKeys(), getCodecTreeFactoryDependency(), getSchemaProvider(), getMonitoredRouter());
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

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
