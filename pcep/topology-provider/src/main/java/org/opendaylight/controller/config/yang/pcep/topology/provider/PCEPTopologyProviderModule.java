/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * Generated file

 * Generated from: yang module name: config-pcep-topology-provider  yang module local name: pcep-topology-impl
 * Generated by: org.opendaylight.controller.config.yangjmxgenerator.plugin.JMXGenerator
 * Generated at: Mon Nov 18 21:08:29 CET 2013
 *
 * Do not modify this file unless it is present under src/main directory
 */
package org.opendaylight.controller.config.yang.pcep.topology.provider;

import static org.opendaylight.bgpcep.pcep.topology.provider.config.PCEPTopologyProviderUtil.contructKeys;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Reflection;
import io.netty.channel.epoll.Epoll;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.opendaylight.bgpcep.pcep.topology.provider.PCEPTopologyProvider;
import org.opendaylight.bgpcep.pcep.topology.provider.config.PCEPTopologyDeployer;
import org.opendaylight.bgpcep.topology.DefaultTopologyReference;
import org.opendaylight.bgpcep.topology.TopologyReference;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.osgi.framework.BundleContext;

/**
 * @deprecated
 */
public final class PCEPTopologyProviderModule extends
    org.opendaylight.controller.config.yang.pcep.topology.provider.AbstractPCEPTopologyProviderModule {

    private static final String IS_NOT_SET = "is not set.";
    private static final String NATIVE_TRANSPORT_NOT_AVAILABLE = "Client is configured with password but native" +
        " transport is not available";
    private BundleContext bundleContext;

    public PCEPTopologyProviderModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
        final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public PCEPTopologyProviderModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
        final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
        final PCEPTopologyProviderModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        JmxAttributeValidationException.checkNotNull(getTopologyId(), IS_NOT_SET, topologyIdJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getListenAddress(), IS_NOT_SET, listenAddressJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getListenPort(), IS_NOT_SET, listenPortJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getStatefulPlugin(), IS_NOT_SET, statefulPluginJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getRpcTimeout(), IS_NOT_SET, rpcTimeoutJmxAttribute);

        final KeyMapping keys = contructKeys(getClient());
        if (!keys.isEmpty()) {
            JmxAttributeValidationException.checkCondition(Epoll.isAvailable(), NATIVE_TRANSPORT_NOT_AVAILABLE,
                clientJmxAttribute);
        }
    }

    private InetAddress listenAddress() {
        final IpAddress a = getListenAddress();
        Preconditions.checkArgument(a.getIpv4Address() != null || a.getIpv6Address() != null,
            "Address %s not supported", a);
        if (a.getIpv4Address() != null) {
            return InetAddresses.forString(a.getIpv4Address().getValue());
        }
        return InetAddresses.forString(a.getIpv6Address().getValue());
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final WaitingServiceTracker<PCEPTopologyDeployer> pcepcTopologyDeployerTracker =
            WaitingServiceTracker.create(PCEPTopologyDeployer.class, this.bundleContext);
        final PCEPTopologyDeployer pcepcTopologyDeployer = pcepcTopologyDeployerTracker
            .waitForService(WaitingServiceTracker.FIVE_MINUTES);

        final TopologyId topologyID = getTopologyId();
        final KeyMapping keys = contructKeys(getClient());

        final InetSocketAddress inetSocketAddress = new InetSocketAddress(listenAddress(), getListenPort().getValue());

        pcepcTopologyDeployer.createTopologyProvider(topologyID, inetSocketAddress, getRpcTimeout(), keys,
            getSchedulerDependency(), Optional.fromNullable(getRootRuntimeBeanRegistratorWrapper()));

        final WaitingServiceTracker<DefaultTopologyReference> defaultTopologyReferenceTracker =
            WaitingServiceTracker.create(DefaultTopologyReference.class, this.bundleContext,
                "(" + PCEPTopologyProvider.class.getName() + "=" + topologyID.getValue() + ")");
        final DefaultTopologyReference defaultTopologyReference = defaultTopologyReferenceTracker
            .waitForService(WaitingServiceTracker.FIVE_MINUTES);

        return Reflection.newProxy(PcepTopologyProviderCloseable.class, new AbstractInvocationHandler() {
            @Override
            protected Object handleInvocation(final Object proxy, final Method method, final Object[] args)
                throws Throwable {
                if (method.getName().equals("close")) {
                    pcepcTopologyDeployer.removeTopologyProvider(topologyID);
                    pcepcTopologyDeployerTracker.close();
                    defaultTopologyReferenceTracker.close();
                    return null;
                } else {
                    return method.invoke(defaultTopologyReference, args);
                }
            }
        });
    }

    void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    private interface PcepTopologyProviderCloseable extends TopologyReference, AutoCloseable {

    }
}
