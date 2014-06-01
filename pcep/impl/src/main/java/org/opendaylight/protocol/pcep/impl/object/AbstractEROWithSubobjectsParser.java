/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl.object;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.pcep.spi.EROSubobjectRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;

public abstract class AbstractEROWithSubobjectsParser implements ObjectParser, ObjectSerializer {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractEROWithSubobjectsParser.class);

	private static final int HEADER_LENGTH = 2;

	private final EROSubobjectRegistry subobjReg;

	protected AbstractEROWithSubobjectsParser(final EROSubobjectRegistry subobjReg) {
		this.subobjReg = Preconditions.checkNotNull(subobjReg);
	}

	protected List<Subobject> parseSubobjects(final ByteBuf buffer) throws PCEPDeserializerException {
		Preconditions.checkArgument(buffer != null && buffer.isReadable(), "Array of bytes is mandatory. Can't be null or empty.");
		final List<Subobject> subs = new ArrayList<>();
		while (buffer.isReadable()) {
			boolean loose = ((buffer.getByte(buffer.readerIndex()) & (1 << 7)) != 0) ? true : false;
			int type = (buffer.readByte() & 0xff) & ~(1 << 7);
			int length = UnsignedBytes.toInt(buffer.readByte()) - HEADER_LENGTH;
			if (length > buffer.readableBytes()) {
				throw new PCEPDeserializerException("Wrong length specified. Passed: " + length + "; Expected: <= "
						+ buffer.readableBytes());
			}
			LOG.debug("Attempt to parse subobject from bytes: {}", ByteBufUtil.hexDump(buffer));
			final Subobject sub = this.subobjReg.parseSubobject(type, buffer.slice(buffer.readerIndex(), length), loose);
			if (sub == null) {
				LOG.warn("Unknown subobject type: {}. Ignoring subobject.", type);
			} else {
				LOG.debug("Subobject was parsed. {}", sub);
				subs.add(sub);
			}
			buffer.readerIndex(buffer.readerIndex() + length);
		}
		return subs;
	}

	protected final byte[] serializeSubobject(final List<Subobject> subobjects) {
		final List<byte[]> result = Lists.newArrayList();
		int finalLength = 0;
		for (final Subobject subobject : subobjects) {
			final byte[] bytes = this.subobjReg.serializeSubobject(subobject);
			if (bytes == null) {
				LOG.warn("Could not find serializer for subobject type: {}. Skipping subobject.", subobject.getSubobjectType());
			} else  {
				finalLength += bytes.length;
				result.add(bytes);
			}
		}
		final byte[] resultBytes = new byte[finalLength];
		int byteOffset = 0;
		for (final byte[] b : result) {
			System.arraycopy(b, 0, resultBytes, byteOffset, b.length);
			byteOffset += b.length;
		}
		return resultBytes;
	}
}
