/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Notification;

public interface BGPExtensionProviderContext extends BGPExtensionConsumerContext {
	public AutoCloseable registerAddressFamily(Class<? extends AddressFamily> clazz, int number);
	public AutoCloseable registerSubsequentAddressFamily(Class<? extends SubsequentAddressFamily> clazz, int number);

	public AutoCloseable registerAttributeParser(int attributeType, AttributeParser parser);
	public AutoCloseable registerAttributeSerializer(Class<? extends DataObject> attributeClass, AttributeSerializer serializer);

	public AutoCloseable registerCapabilityParser(int capabilityType, CapabilityParser parser);
	public AutoCloseable registerCapabilitySerializer(Class<? extends CParameters> capabilityClass, CapabilitySerializer serializer);

	public AutoCloseable registerMessageParser(int messageType, MessageParser parser);
	public AutoCloseable registerMessageSerializer(Class<? extends Notification> messageClass, MessageSerializer serializer);

	public AutoCloseable registerNlriParser(Class<? extends AddressFamily> afi, Class<? extends SubsequentAddressFamily> safi, NlriParser parser);
	public AutoCloseable registerNlriSerializer(Class<? extends DataObject> nlriClass, NlriSerializer serializer);

	public AutoCloseable registerParameterParser(int parameterType, ParameterParser parser);
	public AutoCloseable registerParameterSerializer(Class<? extends BgpParameters> paramClass, ParameterSerializer serializer);
}
