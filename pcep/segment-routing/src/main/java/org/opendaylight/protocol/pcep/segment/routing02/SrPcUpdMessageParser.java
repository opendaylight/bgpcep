/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.segment.routing02;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.util.List;
import org.opendaylight.protocol.pcep.ietf.stateful07.Stateful07PCUpdateRequestMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.Updates;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.UpdatesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.pcupd.message.pcupd.message.updates.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev131222.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.Object;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.rp.object.Rp;

public class SrPcUpdMessageParser extends Stateful07PCUpdateRequestMessageParser {

    public SrPcUpdMessageParser(final ObjectRegistry registry) {
        super(registry);
    }

    @Override
    protected void serializeUpdate(final Updates update, final ByteBuf buffer) {
        if (update.getPath() != null && (SrEroUtil.isSegmentRoutingPath(update.getSrp()) || SrEroUtil.isSegmentRoutingPath(update.getPath().getEro()))) {
            serializeObject(update.getSrp(), buffer);
            serializeObject(update.getLsp(), buffer);
            serializeObject(update.getPath().getEro(), buffer);
        } else {
            super.serializeUpdate(update, buffer);
        }
    }

    @Override
    protected Updates getValidUpdates(final List<Object> objects, final List<Message> errors) {
        if (objects.get(0) instanceof Srp && SrEroUtil.isSegmentRoutingPath((Srp) objects.get(0))) {
            boolean isValid = true;
            final Srp srp = (Srp) objects.get(0);
            final UpdatesBuilder builder = new UpdatesBuilder();
            builder.setSrp(srp);
            objects.remove(0);
            if (objects.get(0) instanceof Lsp) {
                builder.setLsp((Lsp) objects.get(0));
                objects.remove(0);
            } else {
                errors.add(createErrorMsg(PCEPErrors.LSP_MISSING, Optional.<Rp>absent()));
                isValid = false;
            }

            final Object obj = objects.get(0);
            if (obj instanceof Ero) {
                final Ero ero = (Ero) obj;
                final PCEPErrors error = SrEroUtil.validateSrEroSubobjects(ero);
                if (error != null) {
                    errors.add(createErrorMsg(error, Optional.<Rp>absent()));
                    isValid = false;
                } else {
                    builder.setPath(new PathBuilder().setEro(ero).build());
                    objects.remove(0);
                }
            } else {
                errors.add(createErrorMsg(PCEPErrors.ERO_MISSING, Optional.<Rp>absent()));
                isValid = false;
            }
            if (isValid) {
                return builder.build();
            }
            return null;
        }
        return super.getValidUpdates(objects, errors);
    }

}
