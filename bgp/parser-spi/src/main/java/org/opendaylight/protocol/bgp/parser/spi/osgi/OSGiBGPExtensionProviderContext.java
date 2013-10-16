/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.osgi;

import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.ParameterParser;
import org.opendaylight.protocol.bgp.parser.spi.ParameterSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.osgi.framework.BundleContext;

final class OSGiBGPExtensionProviderContext extends OSGiBGPExtensionConsumerContext implements BGPExtensionProviderContext {

	OSGiBGPExtensionProviderContext(final BundleContext context) {
		super(context);
	}

	interface ClassRegistration<T> {
		public int getRegisteredNumber();
		public Class<? extends T> getRegisteredClass();
	}

	@Override
	public AutoCloseable registerAddressFamily(final Class<? extends AddressFamily> clazz, final int number) {
		return register(ClassRegistration.class, new ClassRegistration<AddressFamily>() {
			@Override
			public int getRegisteredNumber() {
				return number;
			}
			@Override
			public Class<? extends AddressFamily> getRegisteredClass() {
				return clazz;
			}
		});
	}

	@Override
	public AutoCloseable registerSubsequentAddressFamily(final Class<? extends SubsequentAddressFamily> clazz, final int number) {
		return register(ClassRegistration.class, new ClassRegistration<SubsequentAddressFamily>() {
			@Override
			public int getRegisteredNumber() {
				return number;
			}
			@Override
			public Class<? extends SubsequentAddressFamily> getRegisteredClass() {
				return clazz;
			}
		});
	}

	@Override
	public AutoCloseable registerAttributeParser(final int attributeType, final AttributeParser parser) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerAttributeSerializer(final Class<? extends DataObject> attributeClass, final AttributeSerializer serializer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerCapabilityParser(final int capabilityType, final CapabilityParser parser) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerCapabilitySerializer(final Class<? extends CParameters> capabilityClass, final CapabilitySerializer serializer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerMessageParser(final int messageType, final MessageParser parser) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerMessageSerializer(final Class<? extends Notification> messageClass, final MessageSerializer serializer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerNlriParser(final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi, final NlriParser parser) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerNlriSerializer(final Class<? extends DataObject> nlriClass, final NlriSerializer serializer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerParameterParser(final int parameterType, final ParameterParser parser) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AutoCloseable registerParameterSerializer(final Class<? extends BgpParameters> paramClass, final ParameterSerializer serializer) {
		// TODO Auto-generated method stub
		return null;
	}
}
