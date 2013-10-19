/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import java.util.ServiceLoader;

import org.opendaylight.protocol.bgp.parser.spi.AddressFamilyRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityParser;
import org.opendaylight.protocol.bgp.parser.spi.CapabilityRegistry;
import org.opendaylight.protocol.bgp.parser.spi.CapabilitySerializer;
import org.opendaylight.protocol.bgp.parser.spi.MessageParser;
import org.opendaylight.protocol.bgp.parser.spi.MessageRegistry;
import org.opendaylight.protocol.bgp.parser.spi.MessageSerializer;
import org.opendaylight.protocol.bgp.parser.spi.NlriParser;
import org.opendaylight.protocol.bgp.parser.spi.NlriRegistry;
import org.opendaylight.protocol.bgp.parser.spi.NlriSerializer;
import org.opendaylight.protocol.bgp.parser.spi.ParameterParser;
import org.opendaylight.protocol.bgp.parser.spi.ParameterRegistry;
import org.opendaylight.protocol.bgp.parser.spi.ParameterSerializer;
import org.opendaylight.protocol.bgp.parser.spi.SubsequentAddressFamilyRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.BgpParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.open.bgp.parameters.CParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Notification;

public final class BGPExtensionProviderContextImpl implements BGPExtensionProviderContext {
	private static final class Holder {
		private static final BGPExtensionConsumerContext INSTANCE;

		static {
			try {
				INSTANCE = BGPExtensionProviderContextImpl.create();
			} catch (Exception e) {
				throw new ExceptionInInitializerError(e);
			}
		}
	}

	private final SimpleAddressFamilyRegistry afiReg = new SimpleAddressFamilyRegistry();
	private final SimpleAttributeRegistry attrReg = new SimpleAttributeRegistry();
	private final SimpleCapabilityRegistry capReg = new SimpleCapabilityRegistry();
	private final SimpleMessageRegistry msgReg = new SimpleMessageRegistry();
	private final SimpleSubsequentAddressFamilyRegistry safiReg = new SimpleSubsequentAddressFamilyRegistry();
	private final SimpleParameterRegistry paramReg = new SimpleParameterRegistry();
	private final SimpleNlriRegistry nlriReg = new SimpleNlriRegistry(afiReg, safiReg);

	private BGPExtensionProviderContextImpl() {

	}

	public static BGPExtensionConsumerContext getSingletonInstance() {
		return Holder.INSTANCE;
	}

	public static BGPExtensionConsumerContext create() throws Exception {
		final BGPExtensionProviderContextImpl ctx = new BGPExtensionProviderContextImpl();

		final ServiceLoader<BGPExtensionProviderActivator> loader = ServiceLoader.load(BGPExtensionProviderActivator.class);
		for (BGPExtensionProviderActivator a : loader) {
			a.start(ctx);
		}

		return ctx;
	}

	@Override
	public AddressFamilyRegistry getAddressFamilyRegistry() {
		return afiReg;
	}

	@Override
	public AutoCloseable registerAddressFamily(final Class<? extends AddressFamily> clazz, final int number) {
		return afiReg.registerAddressFamily(clazz, number);
	}

	@Override
	public AttributeRegistry getAttributeRegistry() {
		return attrReg;
	}

	@Override
	public AutoCloseable registerAttributeParser(final int attributeType, final AttributeParser parser) {
		return attrReg.registerAttributeParser(attributeType, parser);
	}

	@Override
	public AutoCloseable registerAttributeSerializer(final Class<? extends DataObject> attributeClass, final AttributeSerializer serializer) {
		return attrReg.registerAttributeSerializer(attributeClass, serializer);
	}

	@Override
	public CapabilityRegistry getCapabilityRegistry() {
		return capReg;
	}

	@Override
	public AutoCloseable registerCapabilityParser(final int capabilityType, final CapabilityParser parser) {
		return capReg.registerCapabilityParser(capabilityType, parser);
	}

	@Override
	public AutoCloseable registerCapabilitySerializer(final Class<? extends CParameters> capabilityClass, final CapabilitySerializer serializer) {
		return capReg.registerCapabilitySerializer(capabilityClass, serializer);
	}

	@Override
	public MessageRegistry getMessageRegistry() {
		return msgReg;
	}

	@Override
	public AutoCloseable registerMessageParser(final int messageType, final MessageParser parser) {
		return msgReg.registerMessageParser(messageType, parser);
	}

	@Override
	public AutoCloseable registerMessageSerializer(final Class<? extends Notification> messageClass, final MessageSerializer serializer) {
		return msgReg.registerMessageSerializer(messageClass, serializer);
	}

	@Override
	public NlriRegistry getNlriRegistry() {
		return nlriReg;
	}

	@Override
	public AutoCloseable registerNlriParser(final Class<? extends AddressFamily> afi, final Class<? extends SubsequentAddressFamily> safi,
			final NlriParser parser) {
		return nlriReg.registerNlriParser(afi, safi, parser);
	}

	@Override
	public AutoCloseable registerNlriSerializer(final Class<? extends DataObject> nlriClass, final NlriSerializer serializer) {
		throw new UnsupportedOperationException("NLRI serialization not implemented");
	}

	@Override
	public ParameterRegistry getParameterRegistry() {
		return paramReg;
	}

	@Override
	public AutoCloseable registerParameterParser(final int parameterType, final ParameterParser parser) {
		return paramReg.registerParameterParser(parameterType, parser);
	}

	@Override
	public AutoCloseable registerParameterSerializer(final Class<? extends BgpParameters> paramClass, final ParameterSerializer serializer) {
		return paramReg.registerParameterSerializer(paramClass, serializer);
	}

	@Override
	public SubsequentAddressFamilyRegistry getSubsequentAddressFamilyRegistry() {
		return safiReg;
	}

	@Override
	public AutoCloseable registerSubsequentAddressFamily(final Class<? extends SubsequentAddressFamily> clazz, final int number) {
		return safiReg.registerSubsequentAddressFamily(clazz, number);
	}
}
