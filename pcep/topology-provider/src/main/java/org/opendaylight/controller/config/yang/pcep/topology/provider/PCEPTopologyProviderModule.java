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

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opendaylight.bgpcep.pcep.topology.provider.PCEPTopologyProvider;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.controller.config.yang.pcep.impl.PCEPDispatcherImplModuleMXBean;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.tcpmd5.cfg.rev140427.Rfc2385Key;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public final class PCEPTopologyProviderModule extends
        org.opendaylight.controller.config.yang.pcep.topology.provider.AbstractPCEPTopologyProviderModule {
    private static final Logger LOG = LoggerFactory.getLogger(PCEPTopologyProviderModule.class);

    private static final String IS_NOT_SET = "is not set.";

    public PCEPTopologyProviderModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public PCEPTopologyProviderModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final PCEPTopologyProviderModule oldModule,
            final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    private Optional<KeyMapping> contructKeys() {
        KeyMapping ret = null;
        final List<Client> clients = getClient();

        if (clients != null && !clients.isEmpty()) {
            ret = new KeyMapping();
            for (final Client c : clients) {
                if (c.getAddress() == null) {
                    LOG.warn("Client {} does not have an address skipping it", c);
                    continue;
                }
                final Rfc2385Key rfc2385KeyPassword = c.getPassword();
                if (rfc2385KeyPassword != null && !rfc2385KeyPassword.getValue().isEmpty()) {
                    final String s = getAddressString(c.getAddress());
                    ret.put(InetAddresses.forString(s), rfc2385KeyPassword.getValue().getBytes(Charsets.US_ASCII));
                }
            }
        }
        return Optional.fromNullable(ret);
    }


    private String getAddressString(final IpAddress address) {
        Preconditions.checkArgument(address.getIpv4Address() != null || address.getIpv6Address() != null, "Address %s is invalid", address);
        if (address.getIpv4Address() != null) {
            return address.getIpv4Address().getValue();
        }
        return address.getIpv6Address().getValue();
    }


    @Override
    public void customValidation() {
        JmxAttributeValidationException.checkNotNull(getTopologyId(), IS_NOT_SET, topologyIdJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getListenAddress(), IS_NOT_SET, listenAddressJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getListenPort(), IS_NOT_SET, listenPortJmxAttribute);
        JmxAttributeValidationException.checkNotNull(getStatefulPlugin(), IS_NOT_SET, statefulPluginJmxAttribute);

        final Optional<KeyMapping> keys = contructKeys();
        if (keys.isPresent()) {
            /*
             *  This is a nasty hack, but we don't have another clean solution. We cannot allow
             *  password being set if the injected dispatcher does not have the optional
             *  md5-server-channel-factory set.
             *
             *  FIXME: this is a use case for Module interfaces, e.g. PCEPDispatcherImplModule
             *         should something like isMd5ServerSupported()
             */

            final PCEPDispatcherImplModuleMXBean dispatcherProxy = this.dependencyResolver.newMXBeanProxy(getDispatcher(),
                    PCEPDispatcherImplModuleMXBean.class);
            final boolean md5ServerSupported = dispatcherProxy.getMd5ServerChannelFactory() != null;
            JmxAttributeValidationException.checkCondition(md5ServerSupported,
                    "password is not compatible with selected dispatcher", clientJmxAttribute);

        }
    }

    private InetAddress listenAddress() {
        final IpAddress a = getListenAddress();
        Preconditions.checkArgument(a.getIpv4Address() != null || a.getIpv6Address() != null, "Address %s not supported", a);
        if (a.getIpv4Address() != null) {
            return InetAddresses.forString(a.getIpv4Address().getValue());
        }
        return InetAddresses.forString(a.getIpv6Address().getValue());
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final InstanceIdentifier<Topology> topology = InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class,
                new TopologyKey(getTopologyId())).build();
        final InetSocketAddress address = new InetSocketAddress(listenAddress(), getListenPort().getValue());

        try {
            return PCEPTopologyProvider.create(getDispatcherDependency(), address, contructKeys(), getSchedulerDependency(),
                    getDataProviderDependency(), getRpcRegistryDependency(), topology, getStatefulPluginDependency(),
                    Optional.of(getRootRuntimeBeanRegistratorWrapper()));
        } catch (InterruptedException | ExecutionException | TransactionCommitFailedException | ReadFailedException e) {
            LOG.error("Failed to instantiate topology provider at {}", address, e);
            throw new IllegalStateException("Failed to instantiate provider", e);
        }
    }
}
