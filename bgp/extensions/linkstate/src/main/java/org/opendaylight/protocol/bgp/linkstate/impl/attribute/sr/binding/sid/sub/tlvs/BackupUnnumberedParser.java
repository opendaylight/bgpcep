/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs;

import static org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.UnnumberedEroParser.parseUnnumberedEroCase;
import static org.opendaylight.protocol.bgp.linkstate.impl.attribute.sr.binding.sid.sub.tlvs.UnnumberedEroParser.serializeUnnumberedIdEro;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import org.opendaylight.protocol.bgp.linkstate.spi.BindingSubTlvsParser;
import org.opendaylight.protocol.bgp.linkstate.spi.BindingSubTlvsSerializer;
import org.opendaylight.protocol.bgp.linkstate.spi.TlvUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.linkstate.rev171207.ProtocolId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.BindingSubTlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdBackupEroCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdBackupEroCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.segment.routing.ext.rev151014.binding.sub.tlvs.binding.sub.tlv.UnnumberedInterfaceIdEroCase;

public final class BackupUnnumberedParser implements BindingSubTlvsParser, BindingSubTlvsSerializer {
    private static final int BACKUP_UNNUMBERED_ERO = 1168;

    @Override
    public BindingSubTlv parseSubTlv(final ByteBuf slice, final ProtocolId protocolId) {
        final UnnumberedInterfaceIdEroCase unnumberedBackup = parseUnnumberedEroCase(slice);
        return new UnnumberedInterfaceIdBackupEroCaseBuilder().setLoose(unnumberedBackup.isLoose())
            .setRouterId(unnumberedBackup.getRouterId()).setInterfaceId(unnumberedBackup.getInterfaceId()).build();
    }

    @Override
    public int getType() {
        return BACKUP_UNNUMBERED_ERO;
    }

    @Override
    public void serializeSubTlv(final BindingSubTlv bindingSubTlv, final ByteBuf aggregator) {
        Preconditions.checkArgument(bindingSubTlv instanceof UnnumberedInterfaceIdBackupEroCase, "Wrong BindingSubTlv instance expected", bindingSubTlv);
        final UnnumberedInterfaceIdBackupEroCase unnumberedBackup = (UnnumberedInterfaceIdBackupEroCase) bindingSubTlv;
        TlvUtil.writeTLV(BACKUP_UNNUMBERED_ERO, serializeUnnumberedIdEro(unnumberedBackup.isLoose(), unnumberedBackup.getRouterId(),
            unnumberedBackup.getInterfaceId()), aggregator);
    }
}
