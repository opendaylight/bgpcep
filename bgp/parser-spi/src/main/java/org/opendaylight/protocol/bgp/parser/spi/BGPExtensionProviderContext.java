/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import org.opendaylight.protocol.util.ReferenceCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.open.bgp.parameters.optional.capabilities.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Context for registering providers of the various types of extension points BGP provides. These are then consumed by
 * extension consumers. It also provides access to the context-wide object cache, which extension providers can use to
 * increase the in-memory efficiency when the same objects are created over and over again.
 */
public interface BGPExtensionProviderContext extends BGPExtensionConsumerContext {
    AutoCloseable registerAddressFamily(Class<? extends AddressFamily> clazz, int number);

    AutoCloseable registerSubsequentAddressFamily(Class<? extends SubsequentAddressFamily> clazz, int number);

    AutoCloseable registerAttributeParser(int attributeType, AttributeParser parser);

    AutoCloseable registerAttributeSerializer(Class<? extends DataObject> attributeClass, AttributeSerializer serializer);

    AutoCloseable registerCapabilityParser(int capabilityType, CapabilityParser parser);

    AutoCloseable registerCapabilitySerializer(Class<? extends CParameters> capabilityClass, CapabilitySerializer serializer);

    AutoCloseable registerMessageParser(int messageType, MessageParser parser);

    AutoCloseable registerMessageSerializer(Class<? extends Notification> messageClass, MessageSerializer serializer);

    AutoCloseable registerNlriParser(Class<? extends AddressFamily> afi, Class<? extends SubsequentAddressFamily> safi, NlriParser parser);

    AutoCloseable registerNlriSerializer(Class<? extends DataObject> nlriClass, NlriSerializer serializer);

    AutoCloseable registerParameterParser(int parameterType, ParameterParser parser);

    AutoCloseable registerParameterSerializer(Class<? extends BgpParameters> paramClass, ParameterSerializer serializer);

    /**
     * Get the context-wide cache for a particular object type.
     *
     * @return An object cache instance.
     */
    ReferenceCache getReferenceCache();
}
