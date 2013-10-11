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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.path.attributes.AtomicAggregateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;

import com.google.common.base.Preconditions;

public final class SimpleAttributeRegistry implements AttributeRegistry {
	public static final AttributeRegistry INSTANCE;

	static {
		final AttributeRegistry reg = new SimpleAttributeRegistry();

		reg.registerAttributeParser(1, new AttributeParser() {
			@Override
			public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) throws BGPDocumentedException {
				builder.setOrigin(PathAttributeParser.parseOrigin(bytes));
			}
		});
		reg.registerAttributeParser(2, new AttributeParser() {
			@Override
			public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder)	throws BGPDocumentedException, BGPParsingException {
				builder.setAsPath(PathAttributeParser.parseAsPath(bytes));
			}
		});
		reg.registerAttributeParser(3, new AttributeParser() {
			@Override
			public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) {
				builder.setCNextHop(PathAttributeParser.parseNextHop(bytes));
			}
		});
		reg.registerAttributeParser(4, new AttributeParser() {
			@Override
			public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) {
				builder.setMultiExitDisc(PathAttributeParser.parseMultiExitDisc(bytes));
			}
		});
		reg.registerAttributeParser(5, new AttributeParser() {
			@Override
			public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) {
				builder.setLocalPref(PathAttributeParser.parseLocalPref(bytes));
			}
		});
		reg.registerAttributeParser(6, new AttributeParser() {
			@Override
			public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) {
				builder.setAtomicAggregate(new AtomicAggregateBuilder().build());
			}
		});
		reg.registerAttributeParser(7, new AttributeParser() {
			@Override
			public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) {
				builder.setAggregator(PathAttributeParser.parseAggregator(bytes));
			}
		});
		reg.registerAttributeParser(8, new AttributeParser() {
			@Override
			public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) throws BGPDocumentedException {
				builder.setCommunities(PathAttributeParser.parseCommunities(bytes));
			}
		});
		reg.registerAttributeParser(9, new AttributeParser() {
			@Override
			public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) {
				builder.setOriginatorId(PathAttributeParser.parseOriginatorId(bytes));
			}
		});

		reg.registerAttributeParser(10, new AttributeParser() {
			@Override
			public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) {
				builder.setClusterId(PathAttributeParser.parseClusterList(bytes));
			}
		});

		reg.registerAttributeParser(14, new AttributeParser() {
			@Override
			public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) throws BGPDocumentedException {
				PathAttributeParser.parseMPReach(builder, bytes);
			}
		});
		reg.registerAttributeParser(15, new AttributeParser() {
			@Override
			public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) throws BGPDocumentedException {
				PathAttributeParser.parseMPUnreach(builder, bytes);
			}
		});
		reg.registerAttributeParser(16, new AttributeParser() {
			@Override
			public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) throws BGPDocumentedException {
				builder.setExtendedCommunities(PathAttributeParser.parseExtendedCommunities(bytes));
			}
		});
		reg.registerAttributeParser(17, new AttributeParser() {
			@Override
			public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) {
				// AS4 Aggregator is ignored
			}
		});
		reg.registerAttributeParser(18, new AttributeParser() {
			@Override
			public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) {
				// AS4 Path is ignored
			}
		});

		// FIXME: update to IANA number once it is known
		reg.registerAttributeParser(99, new AttributeParser() {
			@Override
			public void parseAttribute(final byte[] bytes, final PathAttributesBuilder builder) throws BGPParsingException {
				PathAttributeParser.parseLinkState(builder, bytes);
			}
		});

		INSTANCE = reg;
	}

	private final HandlerRegistry<DataObject, AttributeParser, AttributeSerializer> handlers = new HandlerRegistry<>();

	@Override
	public AutoCloseable registerAttributeParser(final int messageType, final AttributeParser parser) {
		Preconditions.checkArgument(messageType >= 0 && messageType <= 255);
		return handlers.registerParser(messageType, parser);
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
		final AttributeSerializer serializer = handlers.getSerializer(attribute.getImplementedInterface());
		if (serializer == null) {
			return null;
		}

		return serializer.serializeAttribute(attribute);
	}
}
