package org.opendaylight.protocol.pcep.impl;

import java.util.BitSet;
import java.util.List;

import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.PCEPDocumentedException;
import org.opendaylight.protocol.pcep.PCEPErrors;
import org.opendaylight.protocol.pcep.UnknownObject;
import org.opendaylight.protocol.pcep.spi.MessageParser;
import org.opendaylight.protocol.pcep.spi.MessageSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectHeaderImpl;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;

public abstract class AbstractMessageParser implements MessageParser, MessageSerializer {

	private final static int COMMON_OBJECT_HEADER_LENGTH = 4;

	private final static int OC_F_LENGTH = 1;
	private final static int OT_FLAGS_MF_LENGTH = 1;
	private final static int OBJ_LENGTH_F_LENGTH = 2;

	private final static int OC_F_OFFSET = 0;
	private final static int OT_FLAGS_MF_OFFSET = OC_F_OFFSET + OC_F_LENGTH;
	private final static int OBJ_LENGTH_F_OFFSET = OT_FLAGS_MF_OFFSET + OT_FLAGS_MF_LENGTH;

	private final static int OT_SF_LENGTH = 4;
	private final static int FLAGS_SF_LENGTH = 4;

	/*
	 * offsets of fields inside of multi-field in bits
	 */
	private final static int OT_SF_OFFSET = 0;
	private final static int FLAGS_SF_OFFSET = OT_SF_OFFSET + OT_SF_LENGTH;

	/*
	 * flags offsets inside multi-filed
	 */
	private final static int P_FLAG_OFFSET = 6;
	private final static int I_FLAG_OFFSET = 7;

	private final ObjectHandlerRegistry registry;

	protected AbstractMessageParser(final ObjectHandlerRegistry registry) {
		this.registry = Preconditions.checkNotNull(registry);
	}

	protected byte[] serializeObject(final Object object) {
		if (object == null) {
			throw new IllegalArgumentException("Null object passed.");
		}

		final ObjectSerializer serializer = this.registry.getObjectSerializer(object);

		final byte[] valueBytes = serializer.serializeObject(object);

		final byte[] retBytes = new byte[COMMON_OBJECT_HEADER_LENGTH + valueBytes.length];

		// objClass
		retBytes[OC_F_OFFSET] = UnsignedBytes.checkedCast(serializer.getObjectClass());

		// objType_flags multi-field
		retBytes[OT_FLAGS_MF_OFFSET] = UnsignedBytes.checkedCast(serializer.getObjectType() << (Byte.SIZE - OT_SF_LENGTH));
		if (object.isProcessingRule()) {
			retBytes[OT_FLAGS_MF_OFFSET] |= 1 << Byte.SIZE - (P_FLAG_OFFSET) - 1;
		}
		if (object.isIgnore()) {
			retBytes[OT_FLAGS_MF_OFFSET] |= 1 << Byte.SIZE - (I_FLAG_OFFSET) - 1;
		}

		// objLength
		System.arraycopy(ByteArray.intToBytes(valueBytes.length + COMMON_OBJECT_HEADER_LENGTH), Integer.SIZE / Byte.SIZE
				- OBJ_LENGTH_F_LENGTH, retBytes, OBJ_LENGTH_F_OFFSET, OBJ_LENGTH_F_LENGTH);

		ByteArray.copyWhole(valueBytes, retBytes, COMMON_OBJECT_HEADER_LENGTH);
		return retBytes;
	}

	protected List<Object> parseObjects(final byte[] bytes) throws PCEPDeserializerException, PCEPDocumentedException {
		int offset = 0;
		final List<Object> objs = Lists.newArrayList();
		while (bytes.length - offset > 0) {
			if (bytes.length - offset < COMMON_OBJECT_HEADER_LENGTH) {
				throw new PCEPDeserializerException("Too few bytes in passed array. Passed: " + (bytes.length - offset) + " Expected: >= "
						+ COMMON_OBJECT_HEADER_LENGTH + ".");
			}

			final int objClass = UnsignedBytes.toInt(bytes[offset]);

			offset += OC_F_LENGTH;

			final int objType = UnsignedBytes.toInt(ByteArray.copyBitsRange(bytes[offset], OT_SF_OFFSET, OT_SF_LENGTH));

			final byte[] flagsBytes = { ByteArray.copyBitsRange(bytes[offset], FLAGS_SF_OFFSET, FLAGS_SF_LENGTH) };

			final BitSet flags = ByteArray.bytesToBitSet(flagsBytes);

			offset += OT_FLAGS_MF_LENGTH;

			final int objLength = ByteArray.bytesToInt(ByteArray.subByte(bytes, offset, OBJ_LENGTH_F_LENGTH));

			if (bytes.length - offset < objLength - COMMON_OBJECT_HEADER_LENGTH) {
				throw new PCEPDeserializerException("Too few bytes in passed array. Passed: " + (bytes.length - offset) + " Expected: >= "
						+ objLength + ".");
			}

			offset += OBJ_LENGTH_F_LENGTH;

			// copy bytes for deeper parsing
			final byte[] bytesToPass = ByteArray.subByte(bytes, offset, objLength - COMMON_OBJECT_HEADER_LENGTH);

			offset += objLength - COMMON_OBJECT_HEADER_LENGTH;

			final ObjectParser parser = this.registry.getObjectParser(objClass, objType);

			final ObjectHeader header = new ObjectHeaderImpl(flags.get(P_FLAG_OFFSET), flags.get(I_FLAG_OFFSET));

			try {
				objs.add(parser.parseObject(header, bytesToPass));
			} catch (final PCEPDocumentedException e) {
				if (e.getError() == PCEPErrors.UNRECOGNIZED_OBJ_CLASS | e.getError() == PCEPErrors.UNRECOGNIZED_OBJ_TYPE
						| e.getError() == PCEPErrors.NOT_SUPPORTED_OBJ_CLASS | e.getError() == PCEPErrors.NOT_SUPPORTED_OBJ_TYPE) {
					objs.add(new UnknownObject(e.getError()));
				} else {
					throw e;
				}
			}
		}
		return objs;
	}
}
