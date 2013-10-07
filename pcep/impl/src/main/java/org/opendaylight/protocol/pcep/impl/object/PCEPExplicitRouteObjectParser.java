/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPObject;
import org.opendaylight.protocol.pcep.spi.AbstractObjectParser;
import org.opendaylight.protocol.pcep.spi.HandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ExplicitRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Tlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.SubobjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Subobjects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.path.definition.ExplicitRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.AsNumberSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.CSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.IpPrefixSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.LabelSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.UnnumberedSubobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.route.subobjects.subobject.type.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.route.subobjects.subobject.type.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.route.subobjects.subobject.type.AsNumberBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.route.subobjects.subobject.type.LabelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.route.subobjects.subobject.type.Label;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.route.subobjects.subobject.type.UnnumberedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev130820.route.subobjects.subobject.type.Unnumbered;

import com.google.common.collect.Lists;

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

		assert !(((ExplicitRouteObject) object).getSubobjects().isEmpty()) : "Empty Explicit Route Object.";

		return serializeSubobject(subobject, loose);
	}
	
	@Override
	public void addSubobject(ExplicitRouteBuilder builder, boolean loose, List<CSubobject> subobjects) {
		List<Subobjects> subs = Lists.newArrayList();
		SubobjectsBuilder b = new SubobjectsBuilder();
		// FIXME: :-(
		//b.setLoose(loose);
		for (CSubobject sub : subobjects) {
			if (sub instanceof IpPrefixSubobject) {
				b.setSubobjectType(new IpPrefixBuilder().setIpPrefix(((IpPrefix)sub).getIpPrefix()).build());
				subs.add(b.build());
			} else if (sub instanceof AsNumberSubobject) {
				b.setSubobjectType(new AsNumberBuilder().setAsNumber((AsNumber)sub).build());
				subs.add(b.build());
			} else if (sub instanceof LabelSubobject) {
				b.setSubobjectType(new LabelBuilder().setLabels(((Label)subobject).getLabels()).build());
				subs.add(b.build());
			} else if (sub instanceof UnnumberedSubobject) {
				b.setSubobjectType(new UnnumberedBuilder().setInterfaceId(((Unnumbered)sub).getInterfaceId()).setRouterId(((Unnumbered)subobject).getRouterId()).build());
				subs.add(b.build());
			}
		}
		builder.setSubobjects(subs);
	}

	@Override
	public int getObjectType() {
		return TYPE;
	}

	@Override
	public int getObjectClass() {
		return CLASS;
	}


}
