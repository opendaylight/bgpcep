/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.spi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.opendaylight.protocol.util.BitArray;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.ObjectHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.BandwidthChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.bandwidth.choice.BasicBandwidthObjectCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.bandwidth.choice.BasicBandwidthObjectCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.bandwidth.choice.ReoptimizationBandwidthObjectCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.bandwidth.choice.ReoptimizationBandwidthObjectCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.bandwidth.choice.basic.bandwidth.object._case.BasicBandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.bandwidth.object.bandwidth.choice.reoptimization.bandwidth.object._case.ReoptimizationBandwidthObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.endpoints.obj.Ipv4EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.endpoints.object.endpoints.obj.Ipv6EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.RequestCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request._case.RequestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.error.type.request._case.request.RpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.vendor.information.objects.VendorInformationObject;

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
    private final VendorInformationObjectRegistry viRegistry;

    protected AbstractMessageParser(final ObjectRegistry registry) {
        this.registry = Preconditions.checkNotNull(registry);
        this.viRegistry = null;
    }

    protected AbstractMessageParser(final ObjectRegistry registry, final VendorInformationObjectRegistry viRegistry) {
        this.registry = Preconditions.checkNotNull(registry);
        this.viRegistry = Preconditions.checkNotNull(viRegistry);
    }

    /**
     * Calls registry to pick up specific object serializer for given object.
     * Checks if the object is not null.
     * @param object Object to be serialized, may be null
     * @param buffer ByteBuf where the object should be serialized
     */
    protected void serializeObject(@Nullable final Object object, final ByteBuf buffer) {
        if (object == null) {
            return;
        }
        this.registry.serializeObject(object, buffer);
    }

    private List<Object> parseObjects(final ByteBuf bytes) throws PCEPDeserializerException {
        final List<Object> objs = new ArrayList<>();
        while (bytes.isReadable()) {
            if (bytes.readableBytes() < COMMON_OBJECT_HEADER_LENGTH) {
                throw new PCEPDeserializerException("Too few bytes in passed array. Passed: " + bytes.readableBytes() + " Expected: >= "
                        + COMMON_OBJECT_HEADER_LENGTH + ".");
            }
            final int objClass = bytes.readUnsignedByte();

            final byte flagsByte = bytes.readByte();
            final BitArray flags = BitArray.valueOf(flagsByte);
            final int objType = UnsignedBytes.toInt(ByteArray.copyBitsRange(flagsByte, OT_SF_OFFSET, OT_SF_LENGTH));
            final int objLength = bytes.readUnsignedShort();

            if (bytes.readableBytes() < objLength - COMMON_OBJECT_HEADER_LENGTH) {
                throw new PCEPDeserializerException("Too few bytes in passed array. Passed: " + bytes.readableBytes() + " Expected: >= "
                        + objLength + ".");
            }
            // copy bytes for deeper parsing
            final ByteBuf bytesToPass = bytes.readSlice(objLength - COMMON_OBJECT_HEADER_LENGTH);

            final ObjectHeader header = new ObjectHeaderImpl(flags.get(PROCESSED), flags.get(IGNORED));

            if (VendorInformationUtil.isVendorInformationObject(objClass, objType)) {
                Preconditions.checkState(this.viRegistry != null);
                final EnterpriseNumber enterpriseNumber = new EnterpriseNumber(bytesToPass.readUnsignedInt());
                final Optional<? extends Object> obj = this.viRegistry.parseVendorInformationObject(enterpriseNumber, header, bytesToPass);
                if (obj.isPresent()) {
                    objs.add(obj.get());
                }
            } else {
                // parseObject is required to return null for P=0 errored objects
                final Object o = this.registry.parseObject(objClass, objType, header, bytesToPass);
                if (o != null) {
                    objs.add(o);
                }
            }
        }

        return objs;
    }

    public static Message createErrorMsg(final PCEPErrors e, final Optional<Rp> rp) {
        final PcerrMessageBuilder msgBuilder = new PcerrMessageBuilder();
        if (rp.isPresent()) {
            new RequestCaseBuilder().setRequest(new RequestBuilder().setRps(Collections.singletonList(new RpsBuilder().setRp(
                    rp.get()).build())).build()).build();
        }
        return new PcerrBuilder().setPcerrMessage(
                msgBuilder.setErrors(Arrays.asList(new ErrorsBuilder().setErrorObject(
                        new ErrorObjectBuilder().setType(e.getErrorType()).setValue(
                                e.getErrorValue()).build()).build())).build()).build();
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

    protected final void serializeVendorInformationObjects(final List<VendorInformationObject> viObjects, final ByteBuf buffer) {
        if (viObjects != null) {
            for (final VendorInformationObject viObject : viObjects) {
                this.viRegistry.serializeVendorInformationObject(viObject, buffer);
            }
        }
    }

    protected final List<VendorInformationObject> addVendorInformationObjects(final List<Object> objects) {
        final List<VendorInformationObject> vendorInfo = new ArrayList<>();
        while (!objects.isEmpty() && objects.get(0) instanceof VendorInformationObject) {
            final VendorInformationObject viObject = (VendorInformationObject) objects.get(0);
            vendorInfo.add(viObject);
            objects.remove(0);
        }
        return vendorInfo;
    }

    protected final void serializeBandwidth(final BandwidthChoice bandwidthChoice, final ByteBuf buffer) {
        if (bandwidthChoice instanceof BasicBandwidthObjectCase) {
            serializeObject(((BasicBandwidthObjectCase) bandwidthChoice).getBasicBandwidthObject(), buffer);
        } else if (bandwidthChoice instanceof ReoptimizationBandwidthObjectCase) {
            serializeObject(((ReoptimizationBandwidthObjectCase) bandwidthChoice).getReoptimizationBandwidthObject(), buffer);
        }
    }

    protected BandwidthChoice addBandwidthChoice(final Object obj) {
        if (obj instanceof ReoptimizationBandwidthObject) {
            return new ReoptimizationBandwidthObjectCaseBuilder().setReoptimizationBandwidthObject((
                (ReoptimizationBandwidthObject) obj)).build();
        }else if (obj instanceof BasicBandwidthObject) {
            return new BasicBandwidthObjectCaseBuilder().setBasicBandwidthObject(((BasicBandwidthObject) obj)).build();
        }
        return null;
    }

    protected void serializeEndPoints(final EndpointsObj endpointsObj, final ByteBuf buffer) {
        if (endpointsObj instanceof Ipv6EndpointsObj) {
            serializeObject(((Ipv6EndpointsObj) endpointsObj), buffer);
        }else if (endpointsObj instanceof Ipv4EndpointsObj){
            serializeObject(((Ipv4EndpointsObj) endpointsObj), buffer);
        }
    }
}
