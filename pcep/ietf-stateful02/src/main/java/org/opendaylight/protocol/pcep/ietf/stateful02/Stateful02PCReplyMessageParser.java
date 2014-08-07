/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.ietf.stateful02;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.opendaylight.protocol.pcep.impl.message.PCEPReplyMessageParser;
import org.opendaylight.protocol.pcep.spi.MessageUtil;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.VendorInformationObjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Replies1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.Replies1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.stateful._02.rev140110.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcrep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.Replies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.RepliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.Result;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.FailureCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.FailureCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.SuccessCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.SuccessCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.failure._case.NoPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success._case.SuccessBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success._case.success.Paths;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success._case.success.PathsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;

/**
 * Parser for {@link Pcrep}
 */
public final class Stateful02PCReplyMessageParser extends PCEPReplyMessageParser {

    public Stateful02PCReplyMessageParser(final ObjectRegistry registry, final VendorInformationObjectRegistry viReg) {
        super(registry, viReg);
    }

    @Override
    public void serializeMessage(final Message message, final ByteBuf out) {
        Preconditions.checkArgument(message instanceof Pcrep, "Wrong instance of Message. Passed instance of %s. Need Pcrep.", message.getClass());
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.PcrepMessage repMsg = ((Pcrep) message).getPcrepMessage();
        if (repMsg.getReplies() == null || repMsg.getReplies().isEmpty()) {
            throw new IllegalArgumentException("Replies cannot be null or empty.");
        }
        ByteBuf buffer = Unpooled.buffer();
        for (final Replies reply : repMsg.getReplies()) {
            if (reply.getRp() == null) {
                throw new IllegalArgumentException("Reply must contain RP object.");
            }
            serializeObject(reply.getRp(), buffer);
            if (reply.getAugmentation(Replies1.class) != null && reply.getAugmentation(Replies1.class).getLsp() != null) {
                serializeObject(reply.getAugmentation(Replies1.class).getLsp(), buffer);
            }
            if (reply.getResult() != null) {
                if (reply.getResult() instanceof FailureCase) {
                    final FailureCase f = ((FailureCase) reply.getResult());
                    serializeObject(f.getNoPath(), buffer);
                    if (f.getLspa() != null) {
                        serializeObject(f.getLspa(), buffer);
                    }
                    if (f.getBandwidth() != null) {
                        serializeObject(f.getBandwidth(), buffer);
                    }
                    if (f.getMetrics() != null && !f.getMetrics().isEmpty()) {
                        for (final Metrics m : f.getMetrics()) {
                            serializeObject(m.getMetric(), buffer);
                        }
                    }
                    if (f.getIro() != null) {
                        serializeObject(f.getIro(), buffer);
                    }
                } else {
                    final SuccessCase s = (SuccessCase) reply.getResult();
                    for (final Paths p : s.getSuccess().getPaths()) {
                        serializeObject(p.getEro(), buffer);
                        if (p.getLspa() != null) {
                            serializeObject(p.getLspa(), buffer);
                        }
                        if (p.getOf() != null) {
                            serializeObject(p.getOf(), buffer);
                        }
                        if (p.getBandwidth() != null) {
                            serializeObject(p.getBandwidth(), buffer);
                        }
                        if (p.getMetrics() != null && !p.getMetrics().isEmpty()) {
                            for (final Metrics m : p.getMetrics()) {
                                serializeObject(m.getMetric(), buffer);
                            }
                        }
                        if (p.getIro() != null) {
                            serializeObject(p.getIro(), buffer);
                        }
                    }
                }
            }
        }
        MessageUtil.formatMessage(TYPE, buffer, out);
    }

    @Override
    protected Replies getValidReply(final List<Object> objects, final List<Message> errors) {
        if (!(objects.get(0) instanceof Rp)) {
            errors.add(createErrorMsg(PCEPErrors.RP_MISSING, Optional.<Rp>absent()));
            return null;
        }
        final Rp rp = (Rp) objects.get(0);
        objects.remove(0);
        Result res = null;
        Lsp lsp = null;
        if (objects.get(0) instanceof Lsp) {
            lsp = (Lsp) objects.get(0);
            objects.remove(0);
        }
        if (!objects.isEmpty()) {
            if (objects.get(0) instanceof NoPath) {
                final NoPath noPath = (NoPath) objects.get(0);
                objects.remove(0);
                final FailureCaseBuilder builder = new FailureCaseBuilder();
                builder.setNoPath(noPath);
                while (!objects.isEmpty()) {
                    parseAttributes(builder, objects);
                }
                res = builder.build();
            } else if (objects.get(0) instanceof Ero) {
                final Ero ero = (Ero) objects.get(0);
                objects.remove(0);
                final SuccessBuilder builder = new SuccessBuilder();
                final List<Paths> paths = Lists.newArrayList();
                final PathsBuilder pBuilder = new PathsBuilder();
                pBuilder.setEro(ero);
                while (!objects.isEmpty()) {
                    parsePath(pBuilder, objects);
                    paths.add(pBuilder.build());
                }
                builder.setPaths(paths);
                res = new SuccessCaseBuilder().setSuccess(builder.build()).build();
            }
        }
        return new RepliesBuilder().setRp(rp).addAugmentation(Replies1.class, new Replies1Builder().setLsp(lsp).build()).setResult(res).build();
    }
}
