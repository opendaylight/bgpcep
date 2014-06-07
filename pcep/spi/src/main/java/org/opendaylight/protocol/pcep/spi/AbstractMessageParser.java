/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

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

public abstract class AbstractMessageParser implements MessageParser, MessageSerializer {

    private static final int COMMON_OBJECT_HEADER_LENGTH = 4;

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

    private final ObjectRegistry registry;

    protected AbstractMessageParser(final ObjectRegistry registry) {
        this.registry = Preconditions.checkNotNull(registry);
    }

    protected byte[] serializeObject(final Object object) {
        if (object == null) {
            return new byte[] {};
        }
        return this.registry.serializeObject(object);
    }

    private List<Object> parseObjects(final ByteBuf bytes) throws PCEPDeserializerException {
        final List<Object> objs = new ArrayList<>();
        while (bytes.isReadable()) {
            if (bytes.readableBytes() < COMMON_OBJECT_HEADER_LENGTH) {
                throw new PCEPDeserializerException("Too few bytes in passed array. Passed: " + bytes.readableBytes() + " Expected: >= "
                        + COMMON_OBJECT_HEADER_LENGTH + ".");
            }
            final int objClass = bytes.readUnsignedByte();

            byte flagsByte = bytes.readByte();
            final int objType = UnsignedBytes.toInt(ByteArray.copyBitsRange(flagsByte, OT_SF_OFFSET, OT_SF_LENGTH));
            final byte[] flagsBytes = { ByteArray.copyBitsRange(flagsByte, FLAGS_SF_OFFSET, FLAGS_SF_LENGTH) };
            final BitSet flags = ByteArray.bytesToBitSet(flagsBytes);

            final int objLength = bytes.readUnsignedShort();

            if (bytes.readableBytes() < objLength - COMMON_OBJECT_HEADER_LENGTH) {
                throw new PCEPDeserializerException("Too few bytes in passed array. Passed: " + bytes.readableBytes() + " Expected: >= "
                        + objLength + ".");
            }
            // copy bytes for deeper parsing
            final ByteBuf bytesToPass = bytes.slice(bytes.readerIndex(), objLength - COMMON_OBJECT_HEADER_LENGTH);

            final ObjectHeader header = new ObjectHeaderImpl(flags.get(P_FLAG_OFFSET), flags.get(I_FLAG_OFFSET));

            // parseObject is required to return null for P=0 errored objects
            final Object o = this.registry.parseObject(objClass, objType, header, bytesToPass);
            if (o != null) {
                objs.add(o);
            }
            bytes.readerIndex(bytes.readerIndex() + objLength - COMMON_OBJECT_HEADER_LENGTH);
        }

        return objs;
    }

    public static Message createErrorMsg(final PCEPErrors e) {
        final PCEPErrorMapping maping = PCEPErrorMapping.getInstance();
        return new PcerrBuilder().setPcerrMessage(
                new PcerrMessageBuilder().setErrors(
                        Arrays.asList(new ErrorsBuilder().setErrorObject(
                                new ErrorObjectBuilder().setType(maping.getFromErrorsEnum(e).getType()).setValue(
                                        maping.getFromErrorsEnum(e).getValue()).build()).build())).build()).build();
    }

    public static Message createErrorMsg(final PCEPErrors e, final Rp rp) {
        final PCEPErrorMapping maping = PCEPErrorMapping.getInstance();
        return new PcerrBuilder().setPcerrMessage(
                new PcerrMessageBuilder().setErrorType(
                        new RequestCaseBuilder().setRequest(
                                new RequestBuilder().setRps(Lists.newArrayList(new RpsBuilder().setRp(rp).build())).build()).build()).setErrors(
                        Arrays.asList(new ErrorsBuilder().setErrorObject(
                                new ErrorObjectBuilder().setType(maping.getFromErrorsEnum(e).getType()).setValue(
                                        maping.getFromErrorsEnum(e).getValue()).build()).build())).build()).build();
    }

    protected abstract Message validate(final List<Object> objects, final List<Message> errors) throws PCEPDeserializerException;

    @Override
    public final Message parseMessage(final ByteBuf buffer, final List<Message> errors) throws PCEPDeserializerException {
        Preconditions.checkNotNull(buffer, "Buffer may not be null");

        // Parse objects first
        final List<Object> objs = parseObjects(buffer);

        // Run validation
        return validate(objs, errors);
    }
}
