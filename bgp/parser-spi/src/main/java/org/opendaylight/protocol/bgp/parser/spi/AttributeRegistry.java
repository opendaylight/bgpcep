/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130918.update.PathAttributes;
import org.opendaylight.yangtools.yang.binding.DataObject;

public interface AttributeRegistry {
	public AutoCloseable registerAttributeParser(int attributeType, AttributeParser parser);
	public AutoCloseable registerAttributeSerializer(Class<? extends DataObject> attributeClass, AttributeSerializer serializer);

	public PathAttributes parseAttributes(final byte[] bytes) throws BGPDocumentedException, BGPParsingException;
	public byte[] serializeAttribute(DataObject attribute);
}
