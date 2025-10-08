/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.protocol.pcep.PCEPDeserializerException;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.pcerr.message.error.type.RequestCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.pcerr.message.error.type.request._case.RequestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev250930.pcerr.message.pcerr.message.error.type.request._case.request.RpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.object.rev250930.vendor.information.objects.VendorInformationObject;
import org.opendaylight.yangtools.yang.common.netty.ByteBufUtils;

public abstract class AbstractMessageParser implements MessageParser, MessageSerializer {
    private static final int COMMON_OBJECT_HEADER_LENGTH = 4;
    private static final int OT_SF_LENGTH = 4;

    /*
     * offsets of fields inside of multi-field in bits
     */
    private static final int OT_SF_OFFSET = 0;
    /*
     * flags offsets inside multi-filed
     */
    private static final int PROCESSED = 6;
    private static final int IGNORED = 7;

    private final ObjectRegistry registry;

    protected AbstractMessageParser(final ObjectRegistry registry) {
        this.registry = requireNonNull(registry);
    }

    /**
     * Calls registry to pick up specific object serializer for given object.
     * Checks if the object is not null.
     * @param object Object to be serialized, may be null
     * @param buffer ByteBuf where the object should be serialized
     */
    protected void serializeObject(final @Nullable Object object, final ByteBuf buffer) {
        if (object != null) {
            registry.serializeObject(object, buffer);
        }
    }

    private Queue<Object> parseObjects(final ByteBuf bytes) throws PCEPDeserializerException {
        final Queue<Object> objs = new ArrayDeque<>();
        while (bytes.isReadable()) {
            if (bytes.readableBytes() < COMMON_OBJECT_HEADER_LENGTH) {
                throw new PCEPDeserializerException("Too few bytes in passed array. Passed: " + bytes.readableBytes()
                    + " Expected: >= " + COMMON_OBJECT_HEADER_LENGTH + ".");
            }
            final int objClass = bytes.readUnsignedByte();

            final byte flagsByte = bytes.readByte();
            final BitArray flags = BitArray.valueOf(flagsByte);
            final int objType = UnsignedBytes.toInt(ByteArray.copyBitsRange(flagsByte, OT_SF_OFFSET, OT_SF_LENGTH));
            final int objLength = bytes.readUnsignedShort();

            if (bytes.readableBytes() < objLength - COMMON_OBJECT_HEADER_LENGTH) {
                throw new PCEPDeserializerException("Too few bytes in passed array. Passed: " + bytes.readableBytes()
                    + " Expected: >= " + objLength + ".");
            }
            // copy bytes for deeper parsing
            final ByteBuf bytesToPass = bytes.readSlice(objLength - COMMON_OBJECT_HEADER_LENGTH);

            final ObjectHeader header = new ObjectHeaderImpl(flags.get(PROCESSED), flags.get(IGNORED));

            if (VendorInformationUtil.isVendorInformationObject(objClass, objType)) {
                final var enterpriseNumber = new EnterpriseNumber(ByteBufUtils.readUint32(bytesToPass));
                registry.parseVendorInformationObject(enterpriseNumber, header, bytesToPass).ifPresent(objs::add);
            } else {
                // parseObject is required to return null for P=0 errored objects
                final Object o = registry.parseObject(objClass, objType, header, bytesToPass);
                if (o != null) {
                    objs.add(o);
                }
            }
        }

        return objs;
    }

    public static Message createErrorMsg(final PCEPErrors err, final Optional<Rp> optRp) {
        final PcerrMessageBuilder msgBuilder = new PcerrMessageBuilder();
        optRp.ifPresent(rp -> {
            msgBuilder.setErrorType(new RequestCaseBuilder()
                .setRequest(new RequestBuilder().setRps(List.of(new RpsBuilder().setRp(rp).build())).build())
                .build());
        });
        return new PcerrBuilder()
            .setPcerrMessage(msgBuilder.setErrors(List.of(new ErrorsBuilder()
                .setErrorObject(new ErrorObjectBuilder()
                    .setType(err.getErrorType())
                    .setValue(err.getErrorValue())
                    .build())
                .build()))
                .build())
            .build();
    }

    protected abstract Message validate(Queue<Object> objects, List<Message> errors)
        throws PCEPDeserializerException;

    @Override
    public final Message parseMessage(final ByteBuf buffer, final List<Message> errors)
            throws PCEPDeserializerException {
        // Parse objects first
        final var objs = parseObjects(requireNonNull(buffer, "Buffer may not be null"));

        // Run validation
        return validate(objs, errors);
    }

    protected final void serializeVendorInformationObjects(final List<VendorInformationObject> viObjects,
            final ByteBuf buffer) {
        if (viObjects != null) {
            for (var viObject : viObjects) {
                registry.serializeVendorInformationObject(viObject, buffer);
            }
        }
    }

    protected static List<VendorInformationObject> addVendorInformationObjects(final Queue<Object> objects) {
        final var vendorInfo = new ArrayList<VendorInformationObject>();
        while (objects.peek() instanceof VendorInformationObject viObject) {
            vendorInfo.add(viObject);
            objects.remove();
        }
        return vendorInfo;
    }
}
