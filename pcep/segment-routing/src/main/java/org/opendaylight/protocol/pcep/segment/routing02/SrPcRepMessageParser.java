/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing02;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import java.util.List;
import org.opendaylight.protocol.pcep.impl.message.PCEPReplyMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.protocol.pcep.spi.VendorInformationObjectRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.lsp.setup.type._01.rev140507.Tlvs2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.Replies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.RepliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.Result;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.SuccessCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.SuccessCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success._case.SuccessBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success._case.success.Paths;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcrep.message.pcrep.message.replies.result.success._case.success.PathsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;

public class SrPcRepMessageParser extends PCEPReplyMessageParser {

    public SrPcRepMessageParser(final ObjectRegistry registry, final VendorInformationObjectRegistry viRegistry) {
        super(registry, viRegistry);
    }

    @Override
    protected void serializeReply(Replies reply, ByteBuf buffer) {
        final Rp rp = reply.getRp();
        if (isSegmentRoutingPath(rp)) {
            serializeObject(rp, buffer);
            if (reply.getResult() instanceof SuccessCase) {
                final SuccessCase s = (SuccessCase) reply.getResult();
                for (final Paths p : s.getSuccess().getPaths()) {
                    final Ero ero = p.getEro();
                    serializeObject(ero, buffer);
                }
            }
        } else {
            super.serializeReply(reply, buffer);
        }
    }

    @Override
    protected Replies getValidReply(List<Object> objects, List<Message> errors) {
        if (!(objects.get(0) instanceof Rp)) {
            errors.add(createErrorMsg(PCEPErrors.RP_MISSING, Optional.<Rp>absent()));
            return null;
        }
        final Rp rp = (Rp) objects.get(0);
        if (isSegmentRoutingPath(rp)) {
            objects.remove(0);
            Result res = null;
            if (objects.get(0) instanceof Ero) {
                final SuccessBuilder builder = new SuccessBuilder();
                final List<Paths> paths = Lists.newArrayList();
                final PathsBuilder pBuilder = new PathsBuilder();
                while (!objects.isEmpty()) {
                    final Object object = objects.get(0);
                    if (object instanceof Ero) {
                        final Ero ero = (Ero) object;
                        final PCEPErrors error = SrEroUtil.validateSrEroSubobjects(ero);
                        if (error != null) {
                            errors.add(createErrorMsg(error, Optional.<Rp>absent()));
                            return null;
                        } else {
                            paths.add(pBuilder.setEro(ero).build());
                        }
                    }
                    objects.remove(0);
                }
                builder.setPaths(paths);
                res = new SuccessCaseBuilder().setSuccess(builder.build()).build();
            }
            return new RepliesBuilder().setRp(rp).setResult(res).build();
        }
        return super.getValidReply(objects, errors);
    }

    private boolean isSegmentRoutingPath(final Rp rp) {
        if (rp.getTlvs() != null) {
            return SrEroUtil.isPst(rp.getTlvs().getAugmentation(Tlvs1.class))
                    || SrEroUtil.isPst(rp.getTlvs().getAugmentation(Tlvs2.class));
        }
        return false;
    }
}
