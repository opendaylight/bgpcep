/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.flowspec;

import org.opendaylight.protocol.bgp.flowspec.handlers.FlowspecTypeParser;
import org.opendaylight.protocol.bgp.flowspec.handlers.FlowspecTypeSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.flowspec.rev150807.flowspec.destination.flowspec.FlowspecType;

public class SimpleFlowspecExtensionProviderContext {

    private final SimpleFlowspecTypeRegistry flowspecIpv4TypeRegistry = new SimpleFlowspecTypeRegistry();
    private final SimpleFlowspecTypeRegistry flowspecIpv6TypeRegistry = new SimpleFlowspecTypeRegistry();
    private final SimpleFlowspecTypeRegistry flowspecL3vpnIpv4TypeRegistry = new SimpleFlowspecTypeRegistry();
    private final SimpleFlowspecTypeRegistry flowspecL3vpnIpv6TypeRegistry = new SimpleFlowspecTypeRegistry();

    public AutoCloseable registerFlowspecIpv4TypeParser(final int type, final FlowspecTypeParser parser) {
        return this.flowspecIpv4TypeRegistry.registerFlowspecTypeParser(type, parser);
    }

    public AutoCloseable registerFlowspecIpv4TypeSerializer(final Class<? extends FlowspecType> typeClass, final FlowspecTypeSerializer serializer) {
        return this.flowspecIpv4TypeRegistry.registerFlowspecTypeSerializer(typeClass, serializer);
    }

    public AutoCloseable registerFlowspecIpv6TypeParser(final int type, final FlowspecTypeParser parser) {
        return this.flowspecIpv6TypeRegistry.registerFlowspecTypeParser(type, parser);
    }

    public AutoCloseable registerFlowspecIpv6TypeSerializer(final Class<? extends FlowspecType> typeClass, final FlowspecTypeSerializer serializer) {
        return this.flowspecIpv6TypeRegistry.registerFlowspecTypeSerializer(typeClass, serializer);
    }

    public AutoCloseable registerFlowspecL3vpnIpv4TypeParser(final int type, final FlowspecTypeParser parser) {
        return this.flowspecL3vpnIpv4TypeRegistry.registerFlowspecTypeParser(type, parser);
    }

    public AutoCloseable registerFlowspecL3vpnIpv4TypeSerializer(final Class<? extends FlowspecType> typeClass, final FlowspecTypeSerializer serializer) {
        return this.flowspecL3vpnIpv4TypeRegistry.registerFlowspecTypeSerializer(typeClass, serializer);
    }

    public AutoCloseable registerFlowspecL3vpnIpv6TypeParser(final int type, final FlowspecTypeParser parser) {
        return this.flowspecL3vpnIpv6TypeRegistry.registerFlowspecTypeParser(type, parser);
    }

    public AutoCloseable registerFlowspecL3vpnIpv6TypeSerializer(final Class<? extends FlowspecType> typeClass, final FlowspecTypeSerializer serializer) {
        return this.flowspecL3vpnIpv6TypeRegistry.registerFlowspecTypeSerializer(typeClass, serializer);
    }

    public SimpleFlowspecTypeRegistry getFlowspecIpv4TypeRegistry() {
        return this.flowspecIpv4TypeRegistry;
    }

    public SimpleFlowspecTypeRegistry getFlowspecIpv6TypeRegistry() {
        return this.flowspecIpv6TypeRegistry;
    }

    public SimpleFlowspecTypeRegistry getFlowspecL3vpnIpv4TypeRegistry() {
        return this.flowspecL3vpnIpv4TypeRegistry;
    }

    public SimpleFlowspecTypeRegistry getFlowspecL3vpnIpv6TypeRegistry() {
        return this.flowspecL3vpnIpv6TypeRegistry;
    }
}
