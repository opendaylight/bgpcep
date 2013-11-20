/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.RROSubobjectHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.Rro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.reported.route.object.RroBuilder;

/**
 * Parser for {@link Rro}
 */
public class PCEPReportedRouteObjectParser extends AbstractRROWithSubobjectsParser {

	public static final int CLASS = 8;

	public static final int TYPE = 1;

	public PCEPReportedRouteObjectParser(final RROSubobjectHandlerRegistry subobjReg) {
		super(subobjReg);
	}

	@Override
	public Rro parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("Byte array is mandatory. Can't be null or empty.");
		}
		final RroBuilder builder = new RroBuilder();

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());
		builder.setSubobjects(parseSubobjects(bytes));
		return builder.build();
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof Rro)) {
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass()
					+ ". Needed ReportedRouteObject.");
		}

		final Rro obj = (Rro) object;
		assert !(obj.getSubobjects().isEmpty()) : "Empty Reported Route Object.";
		return serializeSubobject(obj.getSubobjects());
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
