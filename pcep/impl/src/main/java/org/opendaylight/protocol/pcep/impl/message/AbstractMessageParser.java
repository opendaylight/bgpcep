package org.opendaylight.protocol.pcep.impl.message;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.opendaylight.protocol.pcep.spi.MessageParser;
import org.opendaylight.protocol.pcep.spi.MessageSerializer;
import org.opendaylight.protocol.pcep.spi.ObjectHandlerRegistry;
import org.opendaylight.protocol.pcep.spi.ObjectParser;
import org.opendaylight.protocol.pcep.spi.ObjectSerializer;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrorMapping;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.RequestCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request._case.RequestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request._case.request.RpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;

abstract class AbstractMessageParser implements MessageParser, MessageSerializer {

	private static final int COMMON_OBJECT_HEADER_LENGTH = 4;

	private static final int OC_F_LENGTH = 1;
	private static final int OT_FLAGS_MF_LENGTH = 1;
	private static final int OBJ_LENGTH_F_LENGTH = 2;

	private static final int OC_F_OFFSET = 0;
	private static final int OT_FLAGS_MF_OFFSET = OC_F_OFFSET + OC_F_LENGTH;
	private static final int OBJ_LENGTH_F_OFFSET = OT_FLAGS_MF_OFFSET + OT_FLAGS_MF_LENGTH;

	private static final int OT_SF_LENGTH = 4;
	private static final int FLAGS_SF_LENGTH = 4;

	/*
	 * offsets of fields inside of multi-field in bits
	 */
	private static final int OT_SF_OFFSET = 0;
	private static final int FLAGS_SF_OFFSET = OT_SF_OFFSET + OT_SF_LENGTH;

	/*
	 * flags offsets inside multi-filed
	 */
	private static final int P_FLAG_OFFSET = 6;
	private static final int I_FLAG_OFFSET = 7;

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
		if (object.isProcessingRule() != null && object.isProcessingRule()) {
			retBytes[OT_FLAGS_MF_OFFSET] |= 1 << Byte.SIZE - (P_FLAG_OFFSET) - 1;
		}
		if (object.isIgnore() != null && object.isIgnore()) {
			retBytes[OT_FLAGS_MF_OFFSET] |= 1 << Byte.SIZE - (I_FLAG_OFFSET) - 1;
		}

		// objLength
		System.arraycopy(ByteArray.intToBytes(valueBytes.length + COMMON_OBJECT_HEADER_LENGTH, OBJ_LENGTH_F_LENGTH), 0, retBytes,
				OBJ_LENGTH_F_OFFSET, OBJ_LENGTH_F_LENGTH);

		ByteArray.copyWhole(valueBytes, retBytes, COMMON_OBJECT_HEADER_LENGTH);
		return retBytes;
	}

	private final List<Object> parseObjects(final byte[] bytes) throws PCEPDeserializerException {
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

			final ObjectParser parser = Preconditions.checkNotNull(this.registry.getObjectParser(objClass, objType));
			final ObjectHeader header = new ObjectHeaderImpl(flags.get(P_FLAG_OFFSET), flags.get(I_FLAG_OFFSET));

			// parseObject is required to return null for P=0 errored objects
			final Object o = parser.parseObject(header, bytesToPass);
			if (o != null) {
				objs.add(o);
			}
		}

		return objs;
	}

	public static Message createErrorMsg(final PCEPErrors e) {
		final PCEPErrorMapping maping = PCEPErrorMapping.getInstance();
		return new PcerrBuilder().setPcerrMessage(
				new PcerrMessageBuilder().setErrors(
						Arrays.asList(new ErrorsBuilder().setErrorObject(
								new ErrorObjectBuilder().setType(maping.getFromErrorsEnum(e).type).setValue(
										maping.getFromErrorsEnum(e).value).build()).build())).build()).build();
	}

	public static Message createErrorMsg(final PCEPErrors e, final Rp rp) {
		final PCEPErrorMapping maping = PCEPErrorMapping.getInstance();
		return new PcerrBuilder().setPcerrMessage(
				new PcerrMessageBuilder().setErrorType(
						new RequestCaseBuilder().setRequest(
								new RequestBuilder().setRps(Lists.newArrayList(new RpsBuilder().setRp(rp).build())).build()).build()).setErrors(
						Arrays.asList(new ErrorsBuilder().setErrorObject(
								new ErrorObjectBuilder().setType(maping.getFromErrorsEnum(e).type).setValue(
										maping.getFromErrorsEnum(e).value).build()).build())).build()).build();
	}

	abstract protected Message validate(final List<Object> objects, final List<Message> errors) throws PCEPDeserializerException;

	@Override
	public final Message parseMessage(final byte[] buffer, final List<Message> errors) throws PCEPDeserializerException {
		Preconditions.checkNotNull(buffer, "Buffer may not be null");

		// Parse objects first
		final List<Object> objs = parseObjects(buffer);

		// Run validation
		return validate(objs, errors);
	}
}
