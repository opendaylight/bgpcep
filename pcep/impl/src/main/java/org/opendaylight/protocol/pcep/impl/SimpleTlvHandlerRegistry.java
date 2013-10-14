/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.protocol.pcep.impl.tlv.LSPIdentifierIPv4TlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LSPIdentifierIPv6TlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LspDbVersionTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LspSymbolicNameTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.LspUpdateErrorTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.NoPathVectorTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OFListTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OrderTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.OverloadedDurationTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.PCEStatefulCapabilityTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.PredundancyGroupTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.RSVPErrorSpecTlvParser;
import org.opendaylight.protocol.pcep.impl.tlv.ReqMissingTlvParser;
import org.opendaylight.protocol.pcep.spi.TlvHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.TlvParser;
import org.opendaylight.protocol.pcep.spi.TlvSerializer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspDbVersionTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspErrorCodeTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.LspIdentifiersTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.NoPathVectorTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OfListTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OrderTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.OverloadDurationTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.PredundancyGroupIdTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ReqMissingTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.RsvpErrorSpecTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.StatefulCapabilityTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.SymbolicPathNameTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yangtools.yang.binding.DataContainer;

import com.google.common.base.Preconditions;

/**
 *
 */
public final class SimpleTlvHandlerRegistry implements TlvHandlerRegistry {
	public static final TlvHandlerRegistry INSTANCE;

	static {
		final TlvHandlerRegistry reg = new SimpleTlvHandlerRegistry();

		reg.registerTlvParser(NoPathVectorTlvParser.TYPE, new NoPathVectorTlvParser());
		reg.registerTlvParser(OverloadedDurationTlvParser.TYPE, new OverloadedDurationTlvParser());
		reg.registerTlvParser(ReqMissingTlvParser.TYPE, new ReqMissingTlvParser());
		reg.registerTlvParser(OFListTlvParser.TYPE, new OFListTlvParser());
		reg.registerTlvParser(OrderTlvParser.TYPE, new OrderTlvParser());
		reg.registerTlvParser(PCEStatefulCapabilityTlvParser.TYPE, new PCEStatefulCapabilityTlvParser());
		reg.registerTlvParser(LspSymbolicNameTlvParser.TYPE, new LspSymbolicNameTlvParser());
		reg.registerTlvParser(LSPIdentifierIPv4TlvParser.TYPE, new LSPIdentifierIPv4TlvParser());
		reg.registerTlvParser(LSPIdentifierIPv6TlvParser.TYPE, new LSPIdentifierIPv6TlvParser());
		reg.registerTlvParser(LspUpdateErrorTlvParser.TYPE, new LspUpdateErrorTlvParser());
		reg.registerTlvParser(RSVPErrorSpecTlvParser.TYPE, new RSVPErrorSpecTlvParser());
		reg.registerTlvParser(LspDbVersionTlvParser.TYPE, new LspDbVersionTlvParser());
		reg.registerTlvParser(PredundancyGroupTlvParser.TYPE, new PredundancyGroupTlvParser());

		reg.registerTlvSerializer(NoPathVectorTlv.class, new NoPathVectorTlvParser());
		reg.registerTlvSerializer(OverloadDurationTlv.class, new OverloadedDurationTlvParser());
		reg.registerTlvSerializer(ReqMissingTlv.class, new ReqMissingTlvParser());
		reg.registerTlvSerializer(OfListTlv.class, new OFListTlvParser());
		reg.registerTlvSerializer(OrderTlv.class, new OrderTlvParser());
		reg.registerTlvSerializer(StatefulCapabilityTlv.class, new PCEStatefulCapabilityTlvParser());
		reg.registerTlvSerializer(SymbolicPathNameTlv.class, new LspSymbolicNameTlvParser());
		reg.registerTlvSerializer(LspIdentifiersTlv.class, new LSPIdentifierIPv4TlvParser());
		reg.registerTlvSerializer(LspErrorCodeTlv.class, new LspUpdateErrorTlvParser());
		reg.registerTlvSerializer(RsvpErrorSpecTlv.class, new RSVPErrorSpecTlvParser());
		reg.registerTlvSerializer(LspDbVersionTlv.class, new LspDbVersionTlvParser());
		reg.registerTlvSerializer(PredundancyGroupIdTlv.class, new PredundancyGroupTlvParser());

		INSTANCE = reg;
	}

	private final HandlerRegistry<DataContainer, TlvParser, TlvSerializer> handlers = new HandlerRegistry<>();

	@Override
	public AutoCloseable registerTlvParser(final int tlvType, final TlvParser parser) {
		Preconditions.checkArgument(tlvType >= 0 && tlvType < 65535);
		return handlers.registerParser(tlvType, parser);
	}

	@Override
	public TlvParser getTlvParser(final int tlvType) {
		return handlers.getParser(tlvType);
	}

	@Override
	public AutoCloseable registerTlvSerializer(final Class<? extends Tlv> tlvClass, final TlvSerializer serializer) {
		return handlers.registerSerializer(tlvClass, serializer);
	}

	@Override
	public TlvSerializer getTlvSerializer(final Tlv tlv) {
		return handlers.getSerializer(tlv.getImplementedInterface());
	}
}
