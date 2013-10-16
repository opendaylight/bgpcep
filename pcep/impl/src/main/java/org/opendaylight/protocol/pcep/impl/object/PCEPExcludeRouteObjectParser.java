/*
* Copyright (c) 2013 Cisco Systems, Inc. and others. All rights reserved.		
*		
* This program and the accompanying materials are made available under the		
* terms of the Eclipse Public License v1.0 which accompanies this distribution,		
* and is available at http://www.eclipse.org/legal/epl-v10.html		
*/
package org.opendaylight.protocol.pcep.impl.object;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.spi.XROSubobjectHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ExcludeRouteObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.svec.XroBuilder;

/**
 * Parser for {@link ExcludeRouteObject}
 */
public final class PCEPExcludeRouteObjectParser extends AbstractXROWithSubobjectsParser {

	public static final int CLASS = 7; // FIXME: to actual value

	public static final int TYPE = 1;

	public PCEPExcludeRouteObjectParser(final XROSubobjectHandlerRegistry registry) {
		super(registry);
	}

	@Override
	public ExcludeRouteObject parseObject(final ObjectHeader header, final byte[] bytes) throws PCEPDeserializerException,
			PCEPDocumentedException {
		if (bytes == null || bytes.length == 0)
			throw new IllegalArgumentException("Byte array is mandatory. Can't be null or empty.");

		final XroBuilder builder = new XroBuilder();

		builder.setIgnore(header.isIgnore());
		builder.setProcessingRule(header.isProcessingRule());
		builder.setSubobjects(parseSubobjects(bytes));
		return builder.build();
	}

	@Override
	public byte[] serializeObject(final Object object) {
		if (!(object instanceof ExcludeRouteObject))
			throw new IllegalArgumentException("Wrong instance of PCEPObject. Passed " + object.getClass() + ". Needed ExcludeRouteObject.");
		final ExcludeRouteObject obj = (ExcludeRouteObject) object;
		assert !(obj.getSubobjects().isEmpty()) : "Empty Excluded Route Object.";
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
