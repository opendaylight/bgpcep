/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.pcc.mock.spi;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.protocol.pcep.spi.PCEPErrors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.Tlvs1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.pcep.sync.optimizations.rev200720.lsp.db.version.tlv.LspDbVersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.initiated.rev200720.pcinitiate.message.pcinitiate.message.Requests;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.OperationalStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.Pcrpt;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.PcrptBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.PlspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.SrpIdNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.SymbolicPathName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.identifiers.tlv.LspIdentifiersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.identifiers.tlv.lsp.identifiers.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.identifiers.tlv.lsp.identifiers.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.object.Lsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.object.LspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.object.lsp.Tlvs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.lsp.object.lsp.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.pcerr.pcerr.message.error.type.StatefulCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.pcerr.pcerr.message.error.type.stateful._case.StatefulBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.pcerr.pcerr.message.error.type.stateful._case.stateful.SrpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.pcrpt.message.PcrptMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.pcrpt.message.pcrpt.message.ReportsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.pcrpt.message.pcrpt.message.reports.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.pcrpt.message.pcrpt.message.reports.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.srp.object.Srp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.srp.object.SrpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf.stateful.rev250328.symbolic.path.name.tlv.SymbolicPathNameBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.Pcerr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.explicit.route.object.ero.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev250602.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.Ipv4ExtendedTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.LspId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.TunnelId;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;

public final class MsgBuilderUtil {
    private MsgBuilderUtil() {
        // Hidden on purpose
    }

    public static Pcrpt createPcRtpMessage(final Lsp lsp, final Optional<Srp> srp, final Path path) {
        return new PcrptBuilder()
                .setPcrptMessage(new PcrptMessageBuilder()
                    .setReports(List.of(new ReportsBuilder()
                        .setLsp(lsp)
                        .setSrp(srp.orElse(null))
                        .setPath(path).build()))
                    .build())
                .build();
    }

    public static Lsp createLsp(final Uint32 plspId, final boolean sync, final Optional<Tlvs> tlvs,
            final boolean isDelegatedLsp, final boolean remove) {
        return new LspBuilder()
                .setAdministrative(true)
                .setDelegate(isDelegatedLsp)
                .setIgnore(false)
                .setOperational(OperationalStatus.Up)
                .setPlspId(new PlspId(plspId))
                .setProcessingRule(false)
                .setRemove(remove)
                .setSync(sync)
                .setTlvs(tlvs.orElse(null))
                .build();
    }

    public static Lsp createLsp(final Uint32 plspId, final boolean sync, final Optional<Tlvs> tlvs,
            final boolean isDelegatedLspe) {
        return createLsp(plspId, sync, tlvs, isDelegatedLspe, false);
    }

    public static Path createPath(final List<Subobject> subobjects) {
        return new PathBuilder()
                .setEro(new EroBuilder().setSubobject(subobjects).build())
                .build();
    }

    public static Srp createSrp(final Uint32 srpId) {
        return new SrpBuilder()
                .setProcessingRule(false)
                .setIgnore(false)
                .setOperationId(new SrpIdNumber(srpId))
                .build();
    }

    public static Path updToRptPath(final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.ietf
            .stateful.rev250328.pcupd.message.pcupd.message.updates.Path path) {
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

    public static Tlvs createLspTlvs(final Uint32 lspId, final boolean symbolicPathName, final String tunnelEndpoint,
            final String tunnelSender, final String extendedTunnelAddress, final Optional<byte[]> symbolicName) {
        return createLspTlvs(lspId, symbolicPathName, tunnelEndpoint, tunnelSender, extendedTunnelAddress, symbolicName,
                Optional.empty());
    }

    public static Tlvs createLspTlvs(final Uint32 lspId, final boolean symbolicPathName, final String tunnelEndpoint,
            final String tunnelSender, final String extendedTunnelAddress, final Optional<byte[]> symbolicName,
            final Optional<Uint64> lspDBVersion) {
        final TlvsBuilder tlvs = new TlvsBuilder().setLspIdentifiers(new LspIdentifiersBuilder()
                .setLspId(new LspId(lspId))
                .setAddressFamily(
                        new Ipv4CaseBuilder().setIpv4(
                                new Ipv4Builder()
                                        .setIpv4TunnelEndpointAddress(new Ipv4AddressNoZone(tunnelEndpoint))
                                        .setIpv4TunnelSenderAddress(new Ipv4AddressNoZone(tunnelSender))
                                        .setIpv4ExtendedTunnelId(
                                                new Ipv4ExtendedTunnelId(extendedTunnelAddress))
                                        .build()).build()).setTunnelId(new TunnelId(Uint16.valueOf(lspId))).build());
        if (symbolicPathName) {
            if (symbolicName.isPresent()) {
                tlvs.setSymbolicPathName(new SymbolicPathNameBuilder()
                    .setPathName(new SymbolicPathName(symbolicName.orElseThrow()))
                    .build());
            } else {
                tlvs.setSymbolicPathName(new SymbolicPathNameBuilder()
                    .setPathName(new SymbolicPathName(getDefaultPathName(tunnelSender, lspId)))
                    .build());
            }
        }

        lspDBVersion.ifPresent(dbVersion -> {
            tlvs.addAugmentation(new Tlvs1Builder()
                .setLspDbVersion(new LspDbVersionBuilder().setLspDbVersionValue(dbVersion).build())
                .build());
        });
        return tlvs.build();
    }

    public static Optional<Tlvs> createLspTlvsEndofSync(final @NonNull Uint64 dbVersion) {
        final Tlvs tlvs = new TlvsBuilder().addAugmentation(new Tlvs1Builder()
            .setLspDbVersion(new LspDbVersionBuilder().setLspDbVersionValue(dbVersion).build()).build()).build();
        return Optional.of(tlvs);
    }

    public static Pcerr createErrorMsg(final @NonNull PCEPErrors pcepErrors, final Uint32 srpId) {
        return new PcerrBuilder()
                .setPcerrMessage(new PcerrMessageBuilder()
                    .setErrorType(new StatefulCaseBuilder()
                        .setStateful(new StatefulBuilder()
                            .setSrps(List.of(new SrpsBuilder()
                                .setSrp(new SrpBuilder()
                                    .setProcessingRule(false)
                                    .setIgnore(false)
                                    .setOperationId(new SrpIdNumber(srpId))
                                    .build())
                                .build()))
                            .build())
                        .build())
                    .setErrors(List.of(new ErrorsBuilder()
                        .setErrorObject(new ErrorObjectBuilder()
                            .setType(pcepErrors.getErrorType())
                            .setValue(pcepErrors.getErrorValue())
                            .build())
                        .build()))
                    .build())
                .build();
    }

    public static Pcerr createErrorMsg(final @NonNull PCEPErrors pcepErrors, final long srpId) {
        return createErrorMsg(pcepErrors, Uint32.valueOf(srpId));
    }

    public static byte[] getDefaultPathName(final String address, final Uint32 lspId) {
        return ("pcc_" + address + "_tunnel_" + lspId).getBytes(StandardCharsets.UTF_8);
    }

}
