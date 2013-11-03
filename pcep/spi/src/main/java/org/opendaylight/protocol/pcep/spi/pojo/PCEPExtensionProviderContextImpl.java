/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi.pojo;

import java.util.ServiceLoader;

import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.protocol.pcep.spi.EROSubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.EROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.EROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.LabelHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.LabelParser;
import org.opendaylight.protocol.pcep.spi.LabelSerializer;
import org.opendaylight.protocol.pcep.spi.MessageHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.MessageParser;
import org.opendaylight.protocol.pcep.spi.MessageSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderActivator;
import org.opendaylight.protocol.pcep.spi.PCEPExtensionProviderContext;
import org.opendaylight.protocol.pcep.spi.RROSubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.RROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.RROSubobjectSerializer;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.protocol.pcep.spi.XROSubobjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.XROSubobjectParser;
import org.opendaylight.protocol.pcep.spi.XROSubobjectSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.basic.explicit.route.subobjects.SubobjectType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.label.subobject.LabelType;

/**
 *
 */
@ThreadSafe
public final class PCEPExtensionProviderContextImpl implements PCEPExtensionProviderContext {
	private static final class Holder {
		private static final PCEPExtensionProviderContext INSTANCE;

		static {
			try {
				INSTANCE = PCEPExtensionProviderContextImpl.create();
			} catch (final Exception e) {
				throw new ExceptionInInitializerError(e);
			}
		}
	}

	private final SimpleLabelHandlerRegistry labelReg = new SimpleLabelHandlerRegistry();
	private final SimpleMessageHandlerRegistry msgReg = new SimpleMessageHandlerRegistry();
	private final SimpleObjectHandlerRegistry objReg = new SimpleObjectHandlerRegistry();
	private final SimpleEROSubobjectHandlerRegistry eroSubReg = new SimpleEROSubobjectHandlerRegistry();
	private final SimpleRROSubobjectHandlerRegistry rroSubReg = new SimpleRROSubobjectHandlerRegistry();
	private final SimpleXROSubobjectHandlerRegistry xroSubReg = new SimpleXROSubobjectHandlerRegistry();
	private final SimpleTlvHandlerRegistry tlvReg = new SimpleTlvHandlerRegistry();

	protected PCEPExtensionProviderContextImpl() {

	}

	public static PCEPExtensionProviderContext create() throws Exception {
		final PCEPExtensionProviderContext ctx = new PCEPExtensionProviderContextImpl();

		final ServiceLoader<PCEPExtensionProviderActivator> loader = ServiceLoader.load(PCEPExtensionProviderActivator.class);
		for (final PCEPExtensionProviderActivator a : loader) {
			a.start(ctx);
		}

		return ctx;

	}

	public static PCEPExtensionProviderContext getSingletonInstance() {
		return Holder.INSTANCE;
	}

	@Override
	public LabelHandlerRegistry getLabelHandlerRegistry() {
		return this.labelReg;
	}

	@Override
	public MessageHandlerRegistry getMessageHandlerRegistry() {
		return this.msgReg;
	}

	@Override
	public ObjectHandlerRegistry getObjectHandlerRegistry() {
		return this.objReg;
	}

	@Override
	public EROSubobjectHandlerRegistry getEROSubobjectHandlerRegistry() {
		return this.eroSubReg;
	}

	@Override
	public RROSubobjectHandlerRegistry getRROSubobjectHandlerRegistry() {
		return this.rroSubReg;
	}

	@Override
	public XROSubobjectHandlerRegistry getXROSubobjectHandlerRegistry() {
		return this.xroSubReg;
	}

	@Override
	public TlvHandlerRegistry getTlvHandlerRegistry() {
		return this.tlvReg;
	}

	@Override
	public AutoCloseable registerLabelSerializer(final Class<? extends LabelType> labelClass, final LabelSerializer serializer) {
		return this.labelReg.registerLabelSerializer(labelClass, serializer);
	}

	@Override
	public AutoCloseable registerLabelParser(final int cType, final LabelParser parser) {
		return this.labelReg.registerLabelParser(cType, parser);
	}

	@Override
	public AutoCloseable registerEROSubobjectParser(final int subobjectType, final EROSubobjectParser parser) {
		return this.eroSubReg.registerSubobjectParser(subobjectType, parser);
	}

	@Override
	public AutoCloseable registerEROSubobjectSerializer(final Class<? extends SubobjectType> subobjectClass,
			final EROSubobjectSerializer serializer) {
		return this.eroSubReg.registerSubobjectSerializer(subobjectClass, serializer);
	}

	@Override
	public AutoCloseable registerMessageParser(final int messageType, final MessageParser parser) {
		return this.msgReg.registerMessageParser(messageType, parser);
	}

	@Override
	public AutoCloseable registerMessageSerializer(final Class<? extends Message> msgClass, final MessageSerializer serializer) {
		return this.msgReg.registerMessageSerializer(msgClass, serializer);
	}

	@Override
	public AutoCloseable registerObjectParser(final int objectClass, final int objectType, final ObjectParser parser) {
		return this.objReg.registerObjectParser(objectClass, objectType, parser);
	}

	@Override
	public AutoCloseable registerObjectSerializer(final Class<? extends Object> objClass, final ObjectSerializer serializer) {
		return this.objReg.registerObjectSerializer(objClass, serializer);
	}

	@Override
	public AutoCloseable registerRROSubobjectParser(final int subobjectType, final RROSubobjectParser parser) {
		return this.rroSubReg.registerSubobjectParser(subobjectType, parser);
	}

	@Override
	public AutoCloseable registerRROSubobjectSerializer(
			final Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.record.route.subobjects.SubobjectType> subobjectClass,
			final RROSubobjectSerializer serializer) {
		return this.rroSubReg.registerSubobjectSerializer(subobjectClass, serializer);
	}

	@Override
	public AutoCloseable registerTlvParser(final int tlvType, final TlvParser parser) {
		return this.tlvReg.registerTlvParser(tlvType, parser);
	}

	@Override
	public AutoCloseable registerTlvSerializer(final Class<? extends Tlv> tlvClass, final TlvSerializer serializer) {
		return this.tlvReg.registerTlvSerializer(tlvClass, serializer);
	}

	@Override
	public AutoCloseable registerXROSubobjectParser(final int subobjectType, final XROSubobjectParser parser) {
		return this.xroSubReg.registerSubobjectParser(subobjectType, parser);
	}

	@Override
	public AutoCloseable registerXROSubobjectSerializer(final Class<? extends SubobjectType> subobjectClass,
			final XROSubobjectSerializer serializer) {
		return this.xroSubReg.registerSubobjectSerializer(subobjectClass, serializer);
	}
}
