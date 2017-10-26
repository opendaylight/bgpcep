/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.pcep.pcc.mock.spi;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev171025.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.crabbe.initiated.rev171025.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.lsp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcerr.pcerr.message.error.type.StatefulCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcerr.pcerr.message.error.type.stateful._case.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcerr.pcerr.message.error.type.stateful._case.stateful.SrpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.reports.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev131007.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev131005.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.Ipv4ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.TunnelId;

public final class MsgBuilderUtil {

    private MsgBuilderUtil() {
        throw new UnsupportedOperationException();
    }

    public static Pcrpt createPcRtpMessage(final Lsp lsp, final Optional<Srp> srp, final Path path) {
        final PcrptBuilder rptBuilder = new PcrptBuilder();
        final PcrptMessageBuilder msgBuilder = new PcrptMessageBuilder();
        final ReportsBuilder reportBuilder = new ReportsBuilder();
        reportBuilder.setLsp(lsp);
        reportBuilder.setSrp(srp.orNull());
        reportBuilder.setPath(path);
        msgBuilder.setReports(Lists.newArrayList(reportBuilder.build()));
        rptBuilder.setPcrptMessage(msgBuilder.build());
        return rptBuilder.build();
    }

    public static Lsp createLsp(final long plspId, final boolean sync, final Optional<Tlvs> tlvs, final boolean isDelegatedLsp, final boolean remove) {
        final LspBuilder lspBuilder = new LspBuilder();
        lspBuilder.setAdministrative(true);
        lspBuilder.setDelegate(isDelegatedLsp);
        lspBuilder.setIgnore(false);
        lspBuilder.setOperational(OperationalStatus.Up);
        lspBuilder.setPlspId(new PlspId(plspId));
        lspBuilder.setProcessingRule(false);
        lspBuilder.setRemove(remove);
        lspBuilder.setSync(sync);
        lspBuilder.setTlvs(tlvs.orNull());
        return lspBuilder.build();
    }

    public static Lsp createLsp(final long plspId, final boolean sync, final Optional<Tlvs> tlvs, final boolean isDelegatedLspe) {
        return createLsp(plspId, sync, tlvs, isDelegatedLspe, false);
    }

    public static Path createPath(final List<Subobject> subobjects) {
        final PathBuilder pathBuilder = new PathBuilder();
        pathBuilder.setEro(new EroBuilder().setSubobject(subobjects).build());
        return pathBuilder.build();
    }

    public static Srp createSrp(final long srpId) {
        final SrpBuilder srpBuilder = new SrpBuilder();
        srpBuilder.setProcessingRule(false);
        srpBuilder.setIgnore(false);
        srpBuilder.setOperationId(new SrpIdNumber(srpId));
        return srpBuilder.build();
    }

    public static Path updToRptPath(
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev171025.pcupd.message.pcupd.message.updates.Path path) {
        final PathBuilder pathBuilder = new PathBuilder();
        if (path != null) {
            pathBuilder.fieldsFrom(path);
        }
        return pathBuilder.build();
    }

    public static Path reqToRptPath(final Requests request) {
        final PathBuilder pathBuilder = new PathBuilder();
        if (request != null) {
            pathBuilder.fieldsFrom(request);
        }
        return pathBuilder.build();
    }

    public static Tlvs createLspTlvs(final long lspId, final boolean symbolicPathName, final String tunnelEndpoint,
                                     final String tunnelSender, final String extendedTunnelAddress, final Optional<byte[]> symbolicName) {
        return createLspTlvs(lspId, symbolicPathName, tunnelEndpoint, tunnelSender, extendedTunnelAddress, symbolicName, Optional.absent());
    }

    public static Tlvs createLspTlvs(final long lspId, final boolean symbolicPathName, final String tunnelEndpoint,
                                     final String tunnelSender, final String extendedTunnelAddress, final Optional<byte[]> symbolicName, final Optional<BigInteger> lspDBVersion) {
        final TlvsBuilder tlvs = new TlvsBuilder().setLspIdentifiers(new LspIdentifiersBuilder()
                .setLspId(new LspId(lspId))
                .setAddressFamily(
                        new Ipv4CaseBuilder().setIpv4(
                                new Ipv4Builder()
                                        .setIpv4TunnelEndpointAddress(new Ipv4Address(tunnelEndpoint))
                                        .setIpv4TunnelSenderAddress(new Ipv4Address(tunnelSender))
                                        .setIpv4ExtendedTunnelId(
                                                new Ipv4ExtendedTunnelId(extendedTunnelAddress))
                                        .build()).build()).setTunnelId(new TunnelId((int) lspId)).build());
        if (symbolicPathName) {
            if (symbolicName.isPresent()) {
                tlvs.setSymbolicPathName(new SymbolicPathNameBuilder().setPathName(
                        new SymbolicPathName(symbolicName.get())).build());
            } else {
                tlvs.setSymbolicPathName(new SymbolicPathNameBuilder().setPathName(
                        new SymbolicPathName(getDefaultPathName(tunnelSender, lspId))).build());
            }
        }

        if (lspDBVersion.isPresent()) {
            tlvs.addAugmentation(Tlvs1.class, new Tlvs1Builder().setLspDbVersion(new LspDbVersionBuilder().
                setLspDbVersionValue(lspDBVersion.get()).build()).build());
        }
        return tlvs.build();
    }

    public static Optional<Tlvs> createLspTlvsEndofSync(@Nonnull final BigInteger bigInteger) {
        final Tlvs tlvs = new TlvsBuilder().addAugmentation(Tlvs1.class, new Tlvs1Builder().setLspDbVersion(
            new LspDbVersionBuilder().setLspDbVersionValue(bigInteger).build()).build()).build();
        return Optional.of(tlvs);
    }

    public static Pcerr createErrorMsg(@Nonnull final PCEPErrors e, final long srpId) {
        final PcerrMessageBuilder msgBuilder = new PcerrMessageBuilder();
        return new PcerrBuilder().setPcerrMessage(
            msgBuilder
                .setErrorType(
                    new StatefulCaseBuilder().setStateful(
                        new StatefulBuilder().setSrps(
                            Lists.newArrayList(new SrpsBuilder().setSrp(
                                new SrpBuilder().setProcessingRule(false).setIgnore(false)
                                    .setOperationId(new SrpIdNumber(srpId)).build())
                                .build())).build()).build())
                .setErrors(
                    Collections.singletonList(new ErrorsBuilder().setErrorObject(
                        new ErrorObjectBuilder().setType(e.getErrorType()).setValue(e.getErrorValue())
                            .build()).build())).build()).build();
    }

    public static byte[] getDefaultPathName(final String address, final long lspId) {
        return ("pcc_" + address + "_tunnel_" + lspId).getBytes(StandardCharsets.UTF_8);
    }

}
