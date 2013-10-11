/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.impl.message.update;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeRegistry;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.concepts.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;

import com.google.common.base.Preconditions;

public final class SimpleAttributeRegistry implements AttributeRegistry {
	public static final AttributeRegistry INSTANCE;

	static {
		final AttributeRegistry reg = new SimpleAttributeRegistry();

		reg.registerAttributeParser(OriginAttributeParser.TYPE, new OriginAttributeParser());
		reg.registerAttributeParser(AsPathAttributeParser.TYPE, new AsPathAttributeParser());
		reg.registerAttributeParser(NextHopAttributeParser.TYPE, new NextHopAttributeParser());
		reg.registerAttributeParser(MultiExitDiscriminatorAttributeParser.TYPE, new MultiExitDiscriminatorAttributeParser());
		reg.registerAttributeParser(LocalPreferenceAttributeParser.TYPE, new LocalPreferenceAttributeParser());
		reg.registerAttributeParser(AtomicAggregateAttributeParser.TYPE, new AtomicAggregateAttributeParser());
		reg.registerAttributeParser(AggregatorAttributeParser.TYPE, new AggregatorAttributeParser());
		reg.registerAttributeParser(CommunitiesAttributeParser.TYPE, new CommunitiesAttributeParser());
		reg.registerAttributeParser(OriginatorIdAttributeParser.TYPE, new OriginatorIdAttributeParser());
		reg.registerAttributeParser(ClusterIdAttributeParser.TYPE, new ClusterIdAttributeParser());
		reg.registerAttributeParser(MPReachAttributeParser.TYPE, new MPReachAttributeParser());
		reg.registerAttributeParser(MPUnreachAttributeParser.TYPE, new MPUnreachAttributeParser());
		reg.registerAttributeParser(ExtendedCommunitiesAttributeParser.TYPE, new ExtendedCommunitiesAttributeParser());
		reg.registerAttributeParser(AS4AggregatorAttributeParser.TYPE, new AS4AggregatorAttributeParser());
		reg.registerAttributeParser(AS4PathAttributeParser.TYPE, new AS4PathAttributeParser());
		reg.registerAttributeParser(LinkstateAttributeParser.TYPE, new LinkstateAttributeParser());

		INSTANCE = reg;
	}

	private final HandlerRegistry<DataObject, AttributeParser, AttributeSerializer> handlers = new HandlerRegistry<>();

	@Override
	public AutoCloseable registerAttributeParser(final int attributeType, final AttributeParser parser) {
		Preconditions.checkArgument(attributeType >= 0 && attributeType <= 255);
		return handlers.registerParser(attributeType, parser);
	}

	@Override
	public AutoCloseable registerAttributeSerializer(final Class<? extends DataObject> paramClass, final AttributeSerializer serializer) {
		return handlers.registerSerializer(paramClass, serializer);
	}

	@Override
	public boolean parseAttribute(final int attributeType, final byte[] bytes, final PathAttributesBuilder builder) throws BGPDocumentedException, BGPParsingException {
		final AttributeParser parser = handlers.getParser(attributeType);
		if (parser == null) {
			return false;
		}

		parser.parseAttribute(bytes, builder);
		return true;
	}

	@Override
	public byte[] serializeAttribute(final DataObject attribute) {
		final AttributeSerializer serializer = handlers.getSerializer(attribute);
		if (serializer == null) {
			return null;
		}

		return serializer.serializeAttribute(attribute);
	}
}
