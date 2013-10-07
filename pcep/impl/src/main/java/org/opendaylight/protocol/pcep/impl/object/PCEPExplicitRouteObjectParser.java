/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import java.util.Map;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.definition.ExplicitRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CSubobject;

import com.google.common.collect.Maps;

/**
 * Parser for {@link ExplicitRouteObject}
 */
public class PCEPExplicitRouteObjectParser extends AbstractObjectParser<ExplicitRouteBuilder> {

	public static final int CLASS = 7;

	public static final int TYPE = 1;

	public PCEPExplicitRouteObjectParser(final HandlerRegistry registry) {
		super(registry);
	}

	@Override
	public ExplicitRouteObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
			PCEPDocumentedException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Byte array is mandatory. Can't be null or empty.");

		final ExplicitRouteBuilder builder = new ExplicitRouteBuilder();

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());

		parseSubobjects(builder, bytes);
		return builder.build();
	}

	@Override
	public void addTlv(final ExplicitRouteBuilder builder, final Tlv tlv) {
		// No tlvs defined
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof ExplicitRouteObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass()
					+ ". Needed ExplicitRouteObject.");

		final ExplicitRouteObject ero = ((ExplicitRouteObject) object);

		assert !(ero.getSubobjects().isEmpty()) : "Empty Explicit Route Object.";

		final Map<CSubobject, Boolean> subs = Maps.newHashMap();
		for (final Subobjects s : ero.getSubobjects()) {
			subs.put((CSubobject) s, s.isLoose());
		}
		return serializeSubobject(subs);
	}

	// @Override
	// public void addSubobject(ExplicitRouteBuilder builder, Map<CSubobject, Boolean> subobjects) {
	// List<Subobjects> subs = Lists.newArrayList();
	// for (Entry<CSubobject, Boolean> entry : subobjects.entrySet()) {
	// SubobjectsBuilder b = new SubobjectsBuilder();
	// b.setLoose(entry.getValue());
	// CSubobject sub = entry.getKey();
	// if (sub instanceof IpPrefixSubobject) {
	// b.setSubobjectType(new IpPrefixBuilder().setIpPrefix(((IpPrefix)sub).getIpPrefix()).build());
	// subs.add(b.build());
	// } else if (sub instanceof AsNumberSubobject) {
	// b.setSubobjectType(new AsNumberBuilder().setAsNumber((AsNumber)sub).build());
	// subs.add(b.build());
	// } else if (sub instanceof LabelSubobject) {
	// b.setSubobjectType(new LabelBuilder().setLabels(((Label)sub).getLabels()).build());
	// subs.add(b.build());
	// } else if (sub instanceof UnnumberedSubobject) {
	// b.setSubobjectType(new
	// UnnumberedBuilder().setInterfaceId(((Unnumbered)sub).getInterfaceId()).setRouterId(((Unnumbered)sub).getRouterId()).build());
	// subs.add(b.build());
	// }
	// }
	// builder.setSubobjects(subs);
	// }

	@Override
	public int getObjectType() {
		return TYPE;
	}

	@Override
	public int getObjectClass() {
		return CLASS;
	}
}
