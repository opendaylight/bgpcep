/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.parser.message;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.protocol.pcep.spi.MessageUtil;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcmonreq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcmonreqBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.PcreqMessage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.PcreqMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.MonitoringRequest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcreq.message.pcreq.message.Svec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;

/**
 * Parser for {@link Pcmonreq}
 * @see <a href="https://tools.ietf.org/html/rfc5886#section-3.1">Monitoring Request Message</a>
 */
public class PCEPMonitoringRequestMessageParser extends PCEPRequestMessageParser {

    public static final int TYPE = 8;

    public PCEPMonitoringRequestMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        Preconditions.checkArgument(message instanceof Pcmonreq, "Wrong instance of Message. Passed instance of %s. Need Pcmonreq.", message.getClass());
        final PcreqMessage msg = ((Pcmonreq) message).getPcreqMessage();
        Preconditions.checkArgument(msg.getMonitoringRequest() != null, "MONITORING object MUST be present.");
        final ByteBuf buffer = Unpooled.buffer();
        serializeMonitoringRequest(msg.getMonitoringRequest(), buffer);
        if (msg.getSvec() != null) {
            serializeSvec(msg, buffer);
        }
        if (msg.getRequests() != null) {
            serializeRequest(msg, buffer);
        }
        MessageUtil.formatMessage(TYPE, buffer, out);
    }

    @Override
    protected Message validate(final List<Object> objects, final List<Message> errors) throws PCEPDeserializerException {
        Preconditions.checkArgument(objects != null, "Passed list can't be null.");
        if (objects.isEmpty()) {
            throw new PCEPDeserializerException("Pcmonreq message cannot be empty.");
        }
        final MonitoringRequest monReq = getMonitoring(objects);
        if (monReq == null) {
            errors.add(createErrorMsg(PCEPErrors.MONITORING_OBJECT_MISSING, Optional.absent()));
        }
        final PcreqMessageBuilder mBuilder = new PcreqMessageBuilder();
        mBuilder.setMonitoringRequest(monReq);
        final List<Svec> svecs = getSvecs(objects);
        if (!svecs.isEmpty()) {
            mBuilder.setSvec(svecs);
        }
        final List<Requests> requests = getRequests(objects, errors);
        if (requests != null && !requests.isEmpty()) {
            mBuilder.setRequests(requests);
        }
        if (!objects.isEmpty()) {
            throw new PCEPDeserializerException("Unprocessed Objects: " + objects);
        }
        return new PcmonreqBuilder().setPcreqMessage(mBuilder.build()).build();
    }
}
