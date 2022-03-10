/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.pcep.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.protocol.pcep.impl.TestVendorInformationTlvParser.TestEnterpriseSpecificInformation;
import org.opendaylight.protocol.pcep.parser.BaseParserExtensionActivator;
import org.opendaylight.protocol.pcep.parser.message.PCEPCloseMessageParser;
import org.opendaylight.protocol.pcep.parser.message.PCEPErrorMessageParser;
import org.opendaylight.protocol.pcep.parser.message.PCEPKeepAliveMessageParser;
import org.opendaylight.protocol.pcep.parser.message.PCEPMonitoringReplyMessageParser;
import org.opendaylight.protocol.pcep.parser.message.PCEPMonitoringRequestMessageParser;
import org.opendaylight.protocol.pcep.parser.message.PCEPNotificationMessageParser;
import org.opendaylight.protocol.pcep.parser.message.PCEPOpenMessageParser;
import org.opendaylight.protocol.pcep.parser.message.PCEPReplyMessageParser;
import org.opendaylight.protocol.pcep.parser.message.PCEPRequestMessageParser;
import org.opendaylight.protocol.pcep.parser.message.PCEPStartTLSMessageParser;
import org.opendaylight.protocol.pcep.spi.ObjectRegistry;
import org.opendaylight.protocol.pcep.spi.PCEPDeserializerException;
import org.opendaylight.protocol.pcep.spi.pojo.SimplePCEPExtensionProviderContext;
import org.opendaylight.protocol.util.ByteArray;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iana.rev130816.EnterpriseNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ieee754.rev130819.Float32;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.network.concepts.rev131125.Bandwidth;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.CloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.KeepaliveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.OpenBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.PcerrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.PcmonrepBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.PcmonreqBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.PcntfBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.PcrepBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.PcreqBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.message.rev181109.StarttlsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.OfId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.P2mpLeaves;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.ProtocolVersion;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.RequestId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.bandwidth.object.BandwidthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.bnc.object.branch.node.type.BranchNodeCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.branch.node.object.BranchNodeListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.close.message.CCloseMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.close.object.CCloseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.close.object.c.close.TlvsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.Ipv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.P2mpIpv4CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.ipv4._case.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.address.family.p2mp.ipv4._case.P2mpIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.object.EndpointsObj;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.endpoints.object.EndpointsObjBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.Ero;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route.object.EroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.include.route.object.Iro;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.include.route.object.IroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.include.route.object.iro.Subobject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.include.route.object.iro.SubobjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.keepalive.message.KeepaliveMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lsp.attributes.Metrics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lsp.attributes.MetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lspa.object.Lspa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.lspa.object.LspaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.metric.object.MetricBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.monitoring.metrics.MetricPce;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.monitoring.metrics.MetricPceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.monitoring.object.Monitoring;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.monitoring.object.Monitoring.Flags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.monitoring.object.MonitoringBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.monitoring.response.monitoring.metrics.list.GeneralMetricsListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.monitoring.response.monitoring.metrics.list.SpecificMetricsListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.monitoring.response.monitoring.metrics.list.specific.metrics.list.SpecificMetricsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.notification.object.CNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.notification.object.CNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.of.object.Of;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.of.object.OfBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.message.OpenMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object.Open;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.overload.object.Overload;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.overload.object.OverloadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcc.id.req.object.PccIdReq;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcc.id.req.object.PccIdReqBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pce.id.object.PceId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pce.id.object.PceIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcep.error.object.ErrorObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcep.error.object.ErrorObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.PcerrMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.Errors;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.ErrorsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.RequestCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.SessionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.request._case.RequestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message.pcerr.message.error.type.session._case.SessionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcmonrep.message.PcmonrepMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcntf.message.PcntfMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcntf.message.pcntf.message.Notifications;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcntf.message.pcntf.message.NotificationsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcntf.message.pcntf.message.notifications.Rps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcntf.message.pcntf.message.notifications.RpsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.PcrepMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.Replies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.RepliesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.replies.result.FailureCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.replies.result.SuccessCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.replies.result.failure._case.NoPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.replies.result.failure._case.NoPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.replies.result.success._case.SuccessBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.replies.result.success._case.success.Paths;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcrep.message.pcrep.message.replies.result.success._case.success.PathsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.PcreqMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq.message.MonitoringRequestBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq.message.monitoring.request.PceIdList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq.message.monitoring.request.PceIdListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq.message.requests.SegmentComputationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq.message.requests.segment.computation.P2mpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq.message.requests.segment.computation.P2pBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq.message.requests.segment.computation.p2mp.EndpointRroPairBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq.message.requests.segment.computation.p2mp.endpoint.rro.pair.RrosBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq.message.requests.segment.computation.p2mp.endpoint.rro.pair.rros.route.object.ReportedRouteObjectCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq.message.requests.segment.computation.p2mp.endpoint.rro.pair.rros.route.object.SecondaryReportedRouteObjectCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq.message.requests.segment.computation.p2mp.iro.bnc.choice.BncCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.proc.time.object.ProcTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.proc.time.object.ProcTimeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.rp.object.Rp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.rp.object.RpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.secondary.reported.route.object.SrroBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.start.tls.message.StartTlsMessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.svec.object.Svec;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.svec.object.SvecBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.objects.VendorInformationObject;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.vendor.information.objects.VendorInformationObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.AttributeFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route.subobjects.subobject.type.IpPrefixCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820._record.route.subobjects.subobject.type.ip.prefix._case.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.AsNumberCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.rsvp.rev150820.basic.explicit.route.subobjects.subobject.type.as.number._case.AsNumberBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public class PCEPValidatorTest {

    private ObjectRegistry objectRegistry;

    private Open open;
    private Rp rpTrue;
    private Rp rpFalse;
    private NoPath noPath;
    private Lspa lspa;
    private Metrics metrics;
    private Iro iro;
    private Ero ero;
    private Of of;
    private EndpointsObj endpoints;
    private Svec svec;
    private List<VendorInformationObject> viObjects;
    private Monitoring monitoring;
    private PccIdReq pccIdReq;
    private PceId pceId;
    private ProcTime procTime;
    private Overload overload;

    private AsNumberCase eroASSubobject;

    private SimplePCEPExtensionProviderContext ctx;
    private BaseParserExtensionActivator act;
    private TestVendorInformationActivator viObjAct;

    @Before
    public void setUp() {
        ctx = new SimplePCEPExtensionProviderContext();
        act = new BaseParserExtensionActivator();
        viObjAct = new TestVendorInformationActivator();
        act.start(ctx);
        viObjAct.start(ctx);
        objectRegistry = ctx.getObjectHandlerRegistry();
        final RpBuilder rpBuilder = new RpBuilder()
                .setProcessingRule(true)
                .setIgnore(false)
                .setReoptimization(false)
                .setBiDirectional(false)
                .setLoose(true)
                .setMakeBeforeBreak(false)
                .setOrder(false)
                .setPathKey(false)
                .setSupplyOf(false)
                .setFragmentation(false)
                .setP2mp(false)
                .setEroCompression(false)
                .setPriority(Uint8.ONE)
                .setRequestId(new RequestId(Uint32.TEN))
                .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.rp
                    .object.rp.TlvsBuilder().build());
        rpTrue = rpBuilder.build();
        rpFalse = rpBuilder.setProcessingRule(false).build();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object
            .OpenBuilder openBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types
                .rev181109.open.object.OpenBuilder()
                .setProcessingRule(false)
                .setIgnore(false)
                .setDeadTimer(Uint8.ONE)
                .setKeepalive(Uint8.ONE)
                .setSessionId(Uint8.ZERO)
                .setVersion(new ProtocolVersion(Uint8.ONE))
                .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
                    .open.object.open.TlvsBuilder().build());
        // TODO get rid of previous superfluous openBuilder
        open = openBuilder.build();

        noPath = new NoPathBuilder()
                .setProcessingRule(false)
                .setIgnore(false)
                .setNatureOfIssue(Uint8.ZERO)
                .setUnsatisfiedConstraints(false)
                .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
                    .pcrep.message.pcrep.message.replies.result.failure._case.no.path.TlvsBuilder().build())
                .build();

        lspa = new LspaBuilder()
                .setProcessingRule(false)
                .setIgnore(false)
                .setLocalProtectionDesired(false)
                .setHoldPriority(Uint8.ZERO)
                .setSetupPriority(Uint8.ZERO)
                .setExcludeAny(new AttributeFilter(Uint32.ZERO))
                .setIncludeAll(new AttributeFilter(Uint32.ZERO))
                .setIncludeAny(new AttributeFilter(Uint32.ZERO))
                .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
                    .lspa.object.lspa.TlvsBuilder().build())
                .build();

        metrics = new MetricsBuilder()
                .setMetric(new MetricBuilder()
                    .setIgnore(false)
                    .setProcessingRule(false)
                    .setComputed(false)
                    .setBound(false)
                    .setMetricType(Uint8.ONE)
                    .setValue(new Float32(new byte[4]))
                    .build())
                .build();

        eroASSubobject = new AsNumberCaseBuilder()
                .setAsNumber(new AsNumberBuilder()
                    .setAsNumber(new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715
                        .AsNumber(Uint32.valueOf(0xFFFF)))
                    .build())
                .build();

        final IroBuilder iroBuilder = new IroBuilder();
        iroBuilder.setIgnore(false);
        iroBuilder.setProcessingRule(false);
        final List<Subobject> iroSubs = new ArrayList<>();
        iroSubs.add(new SubobjectBuilder().setSubobjectType(eroASSubobject).setLoose(false).build());
        iroBuilder.setSubobject(iroSubs);
        iro = iroBuilder.build();

        final EroBuilder eroBuilder = new EroBuilder();
        eroBuilder.setIgnore(false);
        eroBuilder.setProcessingRule(false);
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit.route
            .object.ero.Subobject> eroSubs = new ArrayList<>();
        eroSubs.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.explicit
            .route.object.ero.SubobjectBuilder().setSubobjectType(eroASSubobject).setLoose(false).build());
        eroBuilder.setSubobject(eroSubs);
        ero = eroBuilder.build();

        of = new OfBuilder()
                .setIgnore(false)
                .setProcessingRule(false)
                .setCode(new OfId(Uint16.ZERO))
                .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.of
                    .object.of.TlvsBuilder().build())
                .build();

        endpoints = new EndpointsObjBuilder()
                .setIgnore(false)
                .setProcessingRule(true)
                .setAddressFamily(new Ipv4CaseBuilder()
                    .setIpv4(new Ipv4Builder()
                        .setSourceIpv4Address(new Ipv4AddressNoZone("255.255.255.255"))
                        .setDestinationIpv4Address(new Ipv4AddressNoZone("255.255.255.255"))
                        .build())
                    .build())
                .build();

        svec = new SvecBuilder()
                .setIgnore(false)
                .setProcessingRule(false)
                .setLinkDiverse(false)
                .setNodeDiverse(false)
                .setSrlgDiverse(false)
                .setLinkDirectionDiverse(false)
                .setPartialPathDiverse(false)
                .setRequestsIds(Set.of(new RequestId(Uint32.ONE)))
                .build();

        viObjects = new ArrayList<>();
        final TestEnterpriseSpecificInformation esInfo = new TestEnterpriseSpecificInformation(5);
        final VendorInformationObject viObj = new VendorInformationObjectBuilder()
                .setEnterpriseNumber(new EnterpriseNumber(Uint32.ZERO))
                .setEnterpriseSpecificInformation(esInfo)
                .build();
        viObjects.add(viObj);

        monitoring = new MonitoringBuilder()
                .setMonitoringId(Uint32.valueOf(100))
                .setFlags(new Flags(false, false, false, false, false))
                .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
                    .monitoring.object.monitoring.TlvsBuilder().build()).build();
        pccIdReq = new PccIdReqBuilder()
                .setIpAddress(new IpAddressNoZone(new Ipv4AddressNoZone("127.0.0.1")))
                .build();
        pceId = new PceIdBuilder()
                .setIpAddress(new IpAddressNoZone(new Ipv4AddressNoZone("127.0.0.2")))
                .build();

        procTime = new ProcTimeBuilder()
                .setEstimated(false)
                .setCurrentProcTime(Uint32.ONE)
                .setMinProcTime(Uint32.TWO)
                .setMaxProcTime(Uint32.valueOf(3))
                .setAverageProcTime(Uint32.valueOf(4))
                .setVarianceProcTime(Uint32.valueOf(5))
                .build();
        overload = new OverloadBuilder().setDuration(Uint16.valueOf(120)).build();
    }

    @Test
    public void testOpenMsg() throws IOException, PCEPDeserializerException {
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPOpenMessage1.bin"));
        final PCEPOpenMessageParser parser = new PCEPOpenMessageParser(ctx.getObjectHandlerRegistry());
        final OpenMessageBuilder builder = new OpenMessageBuilder();

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.open.object
            .OpenBuilder b = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
                .open.object.OpenBuilder()
                .setProcessingRule(false)
                .setIgnore(false)
                .setVersion(new ProtocolVersion(Uint8.ONE))
                .setKeepalive(Uint8.valueOf(30))
                .setDeadTimer(Uint8.valueOf(120))
                .setSessionId(Uint8.ONE)
                .setTlvs(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
                    .open.object.open.TlvsBuilder().build());
        builder.setOpen(b.build());

        assertEquals(new OpenBuilder().setOpenMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.emptyList()));
        final ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new OpenBuilder().setOpenMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        try {
            parser.serializeMessage(new OpenBuilder().setOpenMessage(new OpenMessageBuilder().build()).build(), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Open Object must be present in Open Message.", e.getMessage());
        }
    }

    @Test
    public void testKeepAliveMsg() throws PCEPDeserializerException {
        final ByteBuf result = Unpooled.wrappedBuffer(new byte[] { 32, 2, 0, 4 });
        final PCEPKeepAliveMessageParser parser = new PCEPKeepAliveMessageParser(objectRegistry);
        final KeepaliveBuilder builder = new KeepaliveBuilder()
                .setKeepaliveMessage(new KeepaliveMessageBuilder().build());

        assertEquals(
            builder.build(), parser.parseMessage(result.slice(4, result.readableBytes() - 4), Collections.emptyList()));
        final ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(builder.build(), buf);
        assertArrayEquals(result.array(), buf.array());
    }

    @Test
    public void testStartTLSMsg() throws Exception {
        final ByteBuf result = Unpooled.wrappedBuffer(new byte[] { 32, 20, 0, 4 });
        final PCEPStartTLSMessageParser parser = new PCEPStartTLSMessageParser(objectRegistry);
        final StarttlsBuilder builder = new StarttlsBuilder().setStartTlsMessage(new StartTlsMessageBuilder().build());

        assertEquals(builder.build(), parser.parseMessage(result.slice(4, result.readableBytes() - 4),
            Collections.emptyList()));
        final ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(builder.build(), buf);
        assertArrayEquals(result.array(), buf.array());
    }

    @Test
    public void testCloseMsg() throws IOException, PCEPDeserializerException {
        final ByteBuf result =
            Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPCloseMessage1.bin"));

        final PCEPCloseMessageParser parser = new PCEPCloseMessageParser(objectRegistry);
        final CloseBuilder builder = new CloseBuilder().setCCloseMessage(new CCloseMessageBuilder()
            .setCClose(new CCloseBuilder()
                .setIgnore(false)
                .setProcessingRule(false)
                .setReason(Uint8.valueOf(5))
                .setTlvs(new TlvsBuilder().build())
                .build())
            .build());

        assertEquals(builder.build(), parser.parseMessage(result.slice(4, result.readableBytes() - 4),
            Collections.emptyList()));
        final ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(builder.build(), buf);
        assertArrayEquals(result.array(), buf.array());

        try {
            parser.serializeMessage(new CloseBuilder()
                .setCCloseMessage(new CCloseMessageBuilder().build())
                .build(), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Close Object must be present in Close Message.", e.getMessage());
        }
    }

    @Test
    public void testRequestMsg() throws IOException, PCEPDeserializerException {

        final PCEPRequestMessageParser parser = new PCEPRequestMessageParser(objectRegistry);

        final PcreqMessageBuilder builder = new PcreqMessageBuilder();
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message
            .pcreq.message.Requests> reqs1 = new ArrayList<>();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message
            .pcreq.message.RequestsBuilder rBuilder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message
                    .pcreq.message.RequestsBuilder();
        rBuilder.setRp(rpTrue);
        rBuilder.setSegmentComputation(new SegmentComputationBuilder().setP2p(new P2pBuilder()
            .setEndpointsObj(endpoints).build()).build());
        reqs1.add(rBuilder.build());
        builder.setRequests(reqs1);

        ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCEPRequestMessage1.bin"));
        assertEquals(new PcreqBuilder().setPcreqMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.emptyList()));
        ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcreqBuilder().setPcreqMessage(builder.build()).build(), buf);

        assertArrayEquals(result.array(), buf.array());

        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCReq.3.bin"));

        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message
            .pcreq.message.Requests> reqs2 = new ArrayList<>();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message
            .pcreq.message.RequestsBuilder rBuilder1 =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message
                    .pcreq.message.RequestsBuilder();
        rBuilder1.setRp(rpTrue);
        final P2pBuilder p2pBuilder = new P2pBuilder()
                .setEndpointsObj(endpoints)
                .setMetrics(Lists.newArrayList(metrics))
                .setIro(iro);
        rBuilder1.setSegmentComputation(new SegmentComputationBuilder().setP2p(p2pBuilder.build()).build());
        reqs2.add(rBuilder1.build());
        builder.setRequests(reqs2)
            .setSvec(Lists.newArrayList(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep
                .types.rev181109.pcreq.message.pcreq.message.SvecBuilder().setSvec(svec).build()));

        assertEquals(new PcreqBuilder().setPcreqMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.emptyList()));
        buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcreqBuilder().setPcreqMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCReq.9.bin"));

        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message
            .pcreq.message.Requests> reqs3 = new ArrayList<>();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message
            .pcreq.message.RequestsBuilder rBuilder2 =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message
                    .pcreq.message.RequestsBuilder();
        rBuilder2.setRp(new RpBuilder(rpTrue).setP2mp(true).build());
        final EndpointsObjBuilder epBuilder = new EndpointsObjBuilder()
                .setIgnore(false)
                .setProcessingRule(true)
                .setAddressFamily(new P2mpIpv4CaseBuilder()
                .setP2mpIpv4(new P2mpIpv4Builder()
                        .setP2mpLeaves(P2mpLeaves.NewLeavesToAdd)
                        .setSourceIpv4Address(new Ipv4AddressNoZone("255.255.255.255"))
                        .setDestinationIpv4Address(ImmutableSet.of(new Ipv4AddressNoZone("255.255.255.254"),
                                new Ipv4AddressNoZone("255.255.255.253")))
                        .build()).build());

        final P2mpBuilder p2mpBuilder = new P2mpBuilder();
        p2mpBuilder.setEndpointRroPair(Collections.singletonList(new EndpointRroPairBuilder()
            .setEndpointsObj(epBuilder.build())
            .setRros(Arrays.asList(new RrosBuilder()
                .setRouteObject(new ReportedRouteObjectCaseBuilder()
                    .setRro(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109
                        .reported.route.object.RroBuilder()
                        .setIgnore(false)
                            .setProcessingRule(true)
                            .setSubobject(Arrays.asList(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                                .yang.pcep.types.rev181109.reported.route.object.rro.SubobjectBuilder()
                                .setProtectionAvailable(false)
                                .setProtectionInUse(false)
                                .setSubobjectType(new IpPrefixCaseBuilder()
                                    .setIpPrefix(new IpPrefixBuilder()
                                            .setIpPrefix(new IpPrefix(new Ipv4Prefix("255.255.255.252/32")))
                                            .build())
                                        .build())
                                .build()))
                        .build())
                    .build())
                .build(),
                new RrosBuilder()
                .setRouteObject(new SecondaryReportedRouteObjectCaseBuilder()
                    .setSrro(new SrroBuilder()
                        .setIgnore(false)
                        .setProcessingRule(true)
                        .setSubobject(Arrays.asList(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns
                            .yang.pcep.types.rev181109.secondary.reported.route.object.srro.SubobjectBuilder()
                            .setProtectionInUse(false)
                            .setProtectionAvailable(false)
                            .setSubobjectType(new IpPrefixCaseBuilder()
                                .setIpPrefix(new IpPrefixBuilder()
                                    .setIpPrefix(new IpPrefix(new Ipv4Prefix("255.255.255.251/32")))
                                    .build())
                                .build())
                            .build()))
                        .build())
                    .build())
                .build()))
            .build()));
        p2mpBuilder.setIroBncChoice(new BncCaseBuilder()
            .setBranchNodeType(new BranchNodeCaseBuilder()
                .setBranchNodeList(new BranchNodeListBuilder()
                    .setIgnore(false)
                    .setProcessingRule(true)
                    .setSubobject(Arrays.asList(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang
                        .pcep.types.rev181109.bnc.SubobjectBuilder()
                        .setLoose(false)
                        .setIpPrefix(new IpPrefix(new Ipv4Prefix("255.255.255.252/32")))
                        .build()))
                    .build())
                .build())
            .build());
        rBuilder2.setSegmentComputation(new SegmentComputationBuilder().setP2mp(p2mpBuilder.build()).build());
        reqs3.add(rBuilder2.build());
        builder.setRequests(reqs3).setSvec(null);

        assertEquals(new PcreqBuilder().setPcreqMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                result.readableBytes() - 4), Collections.emptyList()));
        buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcreqBuilder().setPcreqMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        try {
            parser.serializeMessage(new PcreqBuilder().setPcreqMessage(new PcreqMessageBuilder().build()).build(),
                null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Requests cannot be null or empty.", e.getMessage());
        }
        try {
            parser.serializeMessage(new PcreqBuilder().setPcreqMessage(new PcreqMessageBuilder()
                .setRequests(Collections.emptyList()).build()).build(), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Requests cannot be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testReplyMsg() throws IOException, PCEPDeserializerException {
        // only RP

        final PCEPReplyMessageParser parser = new PCEPReplyMessageParser(objectRegistry);

        final PcrepMessageBuilder builder = new PcrepMessageBuilder();
        RepliesBuilder repliesBuilder = new RepliesBuilder();

        final List<Replies> replies1 = new ArrayList<>();
        repliesBuilder.setRp(rpTrue);
        replies1.add(repliesBuilder.build());
        builder.setReplies(replies1);

        ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRep.1.bin"));
        assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.emptyList()));
        ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        // simple Failure
        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRep.2.bin"));
        final List<Replies> replies2 = new ArrayList<>();
        repliesBuilder = new RepliesBuilder();
        repliesBuilder.setRp(rpTrue);
        replies2.add(repliesBuilder.build());
        final RepliesBuilder repliesBuilder2 = new RepliesBuilder();
        repliesBuilder2.setRp(rpTrue);
        repliesBuilder2.setResult(new FailureCaseBuilder().setNoPath(noPath).build());
        replies2.add(repliesBuilder2.build());
        builder.setReplies(replies2);

        assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.emptyList()));
        buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        // Failure with attributes
        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRep.3.bin"));
        final List<Replies> replies3 = new ArrayList<>();
        repliesBuilder = new RepliesBuilder();
        repliesBuilder.setRp(rpTrue);
        repliesBuilder.setResult(new FailureCaseBuilder().setNoPath(noPath).setLspa(lspa)
            .setMetrics(Lists.newArrayList(metrics)).setIro(iro).build());
        replies3.add(repliesBuilder.build());
        builder.setReplies(replies3);

        assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.emptyList()));
        buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        // Success
        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRep.5.bin"));
        final List<Replies> replies4 = new ArrayList<>();
        repliesBuilder = new RepliesBuilder();
        repliesBuilder.setRp(rpTrue);
        final List<Paths> paths = new ArrayList<>();
        final PathsBuilder paBuilder = new PathsBuilder()
                .setEro(ero)
                .setLspa(lspa)
                .setMetrics(Lists.newArrayList(metrics))
                .setIro(iro)
                .setOf(of);
        paths.add(paBuilder.build());
        repliesBuilder.setResult(new SuccessCaseBuilder()
            .setSuccess(new SuccessBuilder().setPaths(paths).build()).build()).build();
        replies4.add(repliesBuilder.build());
        builder.setReplies(replies4);

        assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.emptyList()));
        buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        try {
            parser.serializeMessage(new PcrepBuilder().setPcrepMessage(new PcrepMessageBuilder().build()).build(),
                null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Replies cannot be null or empty.", e.getMessage());
        }
        try {
            parser.serializeMessage(new PcrepBuilder().setPcrepMessage(new PcrepMessageBuilder()
                .setReplies(Collections.emptyList()).build()).build(), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Replies cannot be null or empty.", e.getMessage());
        }
    }

    @Test
    public void testNotificationMsg() throws IOException, PCEPDeserializerException {
        final CNotification cn1 = new CNotificationBuilder().setIgnore(false).setProcessingRule(false)
            .setType(Uint8.ONE).setValue(Uint8.ONE).build();

        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcntf
            .message.pcntf.message.notifications.Notifications> innerNot = new ArrayList<>();
        innerNot.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcntf
            .message.pcntf.message.notifications.NotificationsBuilder().setCNotification(cn1).build());
        final List<Rps> rps = new ArrayList<>();
        rps.add(new RpsBuilder().setRp(rpFalse).build());

        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCNtf.5.bin"));

        final PCEPNotificationMessageParser parser = new PCEPNotificationMessageParser(objectRegistry);
        final PcntfMessageBuilder builder = new PcntfMessageBuilder();

        final List<Notifications> nots = new ArrayList<>();
        final NotificationsBuilder b = new NotificationsBuilder();
        b.setNotifications(innerNot);
        b.setRps(rps);
        nots.add(b.build());

        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcntf
            .message.pcntf.message.notifications.Notifications> innerNot1 = new ArrayList<>();
        innerNot1.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcntf
            .message.pcntf.message.notifications.NotificationsBuilder().setCNotification(cn1).build());
        innerNot1.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcntf
            .message.pcntf.message.notifications.NotificationsBuilder().setCNotification(cn1).build());
        final List<Rps> rps1 = new ArrayList<>();
        rps1.add(new RpsBuilder().setRp(rpFalse).build());
        rps1.add(new RpsBuilder().setRp(rpFalse).build());

        b.setNotifications(innerNot1);
        b.setRps(rps1);
        nots.add(b.build());
        builder.setNotifications(nots);

        assertEquals(new PcntfBuilder().setPcntfMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.emptyList()));
        final ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcntfBuilder().setPcntfMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());
    }

    @Test
    public void testErrorMsg() throws IOException, PCEPDeserializerException {

        ErrorObject error1 = new ErrorObjectBuilder().setIgnore(false).setProcessingRule(false)
                .setType(Uint8.valueOf(3)).setValue(Uint8.ONE).build();

        final PCEPErrorMessageParser parser = new PCEPErrorMessageParser(ctx.getObjectHandlerRegistry());

        List<Errors> innerErr = new ArrayList<>();

        final PcerrMessageBuilder builder = new PcerrMessageBuilder();

        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message
            .pcerr.message.error.type.request._case.request.Rps> rps = new ArrayList<>();
        rps.add(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcerr.message
            .pcerr.message.error.type.request._case.request.RpsBuilder().setRp(rpFalse).build());

        innerErr.add(new ErrorsBuilder().setErrorObject(error1).build());

        builder.setErrors(innerErr);
        builder.setErrorType(new RequestCaseBuilder().setRequest(new RequestBuilder().setRps(rps).build()).build());

        ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCErr.5.bin"));
        assertEquals(new PcerrBuilder().setPcerrMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.emptyList()));
        ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcerrBuilder().setPcerrMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCErr.3.bin"));

        error1 = new ErrorObjectBuilder().setIgnore(false).setProcessingRule(false).setType(Uint8.valueOf(3))
            .setValue(Uint8.ONE).build();

        innerErr = new ArrayList<>();
        innerErr.add(new ErrorsBuilder().setErrorObject(error1).build());

        builder.setErrors(innerErr);
        builder.setErrorType(
            new SessionCaseBuilder().setSession(new SessionBuilder().setOpen(open).build()).build());

        assertEquals(new PcerrBuilder().setPcerrMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.emptyList()));
        buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcerrBuilder().setPcerrMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        try {
            parser.serializeMessage(new PcerrBuilder()
                .setPcerrMessage(new PcerrMessageBuilder().build()).build(), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Errors should not be empty.", e.getMessage());
        }
        try {
            parser.serializeMessage(new PcerrBuilder().setPcerrMessage(new PcerrMessageBuilder()
                .setErrors(Collections.emptyList()).build()).build(), null);
            fail();
        } catch (final IllegalArgumentException e) {
            assertEquals("Errors should not be empty.", e.getMessage());
        }
    }

    @Test
    public void testReqMsgWithVendorInfoObjects() throws IOException, PCEPDeserializerException {
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCReq.7.bin"));
        final PCEPRequestMessageParser parser = new PCEPRequestMessageParser(objectRegistry);

        final PcreqMessageBuilder builder = new PcreqMessageBuilder();
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message
            .pcreq.message.Requests> reqs1 = new ArrayList<>();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq
            .message.RequestsBuilder rBuilder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message
                    .pcreq.message.RequestsBuilder();
        rBuilder.setRp(rpTrue);
        rBuilder.setVendorInformationObject(viObjects);
        final SegmentComputationBuilder sBuilder = new SegmentComputationBuilder();
        sBuilder.setP2p(
            new P2pBuilder().setEndpointsObj(endpoints).setVendorInformationObject(viObjects).build());
        rBuilder.setSegmentComputation(sBuilder.build());
        reqs1.add(rBuilder.build());
        builder.setSvec(Lists.newArrayList(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep
            .types.rev181109.pcreq.message.pcreq.message.SvecBuilder().setSvec(svec)
            .setVendorInformationObject(viObjects).build()));
        builder.setRequests(reqs1);

        assertEquals(new PcreqBuilder().setPcreqMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.emptyList()));
        final ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcreqBuilder().setPcreqMessage(builder.build()).build(), buf);

        assertArrayEquals(result.array(), buf.array());
    }

    @Test
    public void testRepMsgWithVendorInforObjects() throws IOException, PCEPDeserializerException {
        final PCEPReplyMessageParser parser = new PCEPReplyMessageParser(objectRegistry);

        final PcrepMessageBuilder builder = new PcrepMessageBuilder();
        RepliesBuilder repliesBuilder = new RepliesBuilder();
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRep.6.bin"));
        final List<Replies> replies = new ArrayList<>();
        repliesBuilder = new RepliesBuilder()
                .setRp(rpTrue)
                .setVendorInformationObject(viObjects);
        final List<Paths> paths = new ArrayList<>();
        final PathsBuilder paBuilder = new PathsBuilder();
        paBuilder.setEro(ero);
        paths.add(paBuilder.build());
        repliesBuilder.setResult(new SuccessCaseBuilder().setSuccess(new SuccessBuilder().setPaths(paths)
            .setVendorInformationObject(viObjects).build()).build()).build();
        replies.add(repliesBuilder.build());
        builder.setReplies(replies);

        assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.emptyList()));
        final ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());
    }

    @Test
    public void testMonRepMsg() throws PCEPDeserializerException, IOException {
        final PCEPMonitoringReplyMessageParser parser = new PCEPMonitoringReplyMessageParser(objectRegistry);
        final PcmonrepMessageBuilder builder = new PcmonrepMessageBuilder();
        builder.setMonitoring(monitoring)
                .setMonitoringMetricsList(new GeneralMetricsListBuilder()
                .setMetricPce(Lists.newArrayList(new MetricPceBuilder().setPceId(pceId).build())).build());

        final byte[] msgBytes = {
            0x20, 0x09, 0x00, 0x18,
            /* monitoring object */
            0x13, 0x10, 0x00, 0x0C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x64,
            /* pce-id object */
            0x19, 0x10, 0x00, 0x08, 0x7f, 0x00, 0x00, 0x02
        };

        ByteBuf result = Unpooled.wrappedBuffer(msgBytes);
        assertEquals(
            new PcmonrepBuilder().setPcmonrepMessage(builder.build()).build(),
            parser.parseMessage(result.slice(4, result.readableBytes() - 4), Collections.emptyList()));
        ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcmonrepBuilder().setPcmonrepMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        builder.setMonitoring(monitoring);
        builder.setPccIdReq(pccIdReq);
        final SpecificMetricsBuilder smBuilder = new SpecificMetricsBuilder();
        smBuilder.setRp(rpTrue);
        final List<MetricPce> metricPces = new ArrayList<>();
        metricPces.add(new MetricPceBuilder().setOverload(overload).setPceId(pceId)
            .setProcTime(procTime).build());
        metricPces.add(new MetricPceBuilder().setPceId(pceId).setProcTime(procTime).build());
        smBuilder.setMetricPce(metricPces);
        final SpecificMetricsBuilder smBuilder2 = new SpecificMetricsBuilder();
        final List<MetricPce> metricPces2 = new ArrayList<>();
        smBuilder2.setRp(rpTrue);
        metricPces2.add(new MetricPceBuilder().setOverload(overload).setPceId(pceId).build());
        smBuilder2.setMetricPce(metricPces2);
        builder.setMonitoringMetricsList(new SpecificMetricsListBuilder()
            .setSpecificMetrics(Lists.newArrayList(smBuilder.build(), smBuilder2.build())).build());

        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCMonRep.bin"));
        assertEquals(
            new PcmonrepBuilder().setPcmonrepMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                result.readableBytes() - 4), Collections.emptyList()));
        buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcmonrepBuilder().setPcmonrepMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());
    }

    @Test
    public void testRepWithMonitoring() throws IOException, PCEPDeserializerException {
        final PCEPReplyMessageParser parser = new PCEPReplyMessageParser(objectRegistry);

        final PcrepMessageBuilder builder = new PcrepMessageBuilder();
        RepliesBuilder repliesBuilder = new RepliesBuilder();

        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRepMon.5.bin"));
        final List<Replies> replies4 = new ArrayList<>();
        repliesBuilder = new RepliesBuilder()
                .setRp(rpTrue)
                .setMonitoring(monitoring)
                .setPccIdReq(pccIdReq)
                .setMetricPce(Lists.newArrayList(new MetricPceBuilder().setPceId(pceId).build()));
        final List<Paths> paths = new ArrayList<>();
        final PathsBuilder paBuilder = new PathsBuilder()
                .setEro(ero)
                .setLspa(lspa)
                .setMetrics(Lists.newArrayList(metrics))
                .setIro(iro)
                .setOf(of);
        paths.add(paBuilder.build());
        repliesBuilder.setResult(new SuccessCaseBuilder().setSuccess(new SuccessBuilder().setPaths(paths).build())
            .build()).build();
        replies4.add(repliesBuilder.build());
        builder.setReplies(replies4);

        assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.emptyList()));
        final ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());
    }

    @Test
    public void testReqWithMonitoring() throws IOException, PCEPDeserializerException {
        final ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCReq.8.bin"));

        final PCEPRequestMessageParser parser = new PCEPRequestMessageParser(objectRegistry);

        final PcreqMessageBuilder builder = new PcreqMessageBuilder();
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message
            .pcreq.message.Requests> reqs1 = new ArrayList<>();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq
            .message.RequestsBuilder rBuilder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message
                    .pcreq.message.RequestsBuilder();
        rBuilder.setRp(rpTrue)
                .setSegmentComputation(new SegmentComputationBuilder().setP2p(new P2pBuilder()
                .setEndpointsObj(endpoints).build()).build());
        reqs1.add(rBuilder.build());
        final MonitoringRequestBuilder monReqBuilder = new MonitoringRequestBuilder()
                .setMonitoring(monitoring)
                .setPccIdReq(pccIdReq)
                .setPceIdList(Lists.newArrayList(new PceIdListBuilder().setPceId(pceId).build()));
        builder.setMonitoringRequest(monReqBuilder.build()).setRequests(reqs1);

        assertEquals(new PcreqBuilder().setPcreqMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                result.readableBytes() - 4), Collections.emptyList()));
        final ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcreqBuilder().setPcreqMessage(builder.build()).build(), buf);

        assertArrayEquals(result.array(), buf.array());
    }

    @Test
    public void testMonReqMsg() throws PCEPDeserializerException, IOException {
        final PCEPMonitoringRequestMessageParser parser = new PCEPMonitoringRequestMessageParser(objectRegistry);

        final PcreqMessageBuilder builder = new PcreqMessageBuilder();
        final MonitoringRequestBuilder monReqBuilder = new MonitoringRequestBuilder()
                .setMonitoring(monitoring)
                .setPceIdList(Lists.newArrayList(new PceIdListBuilder().setPceId(pceId).build()));
        builder.setMonitoringRequest(monReqBuilder.build());
        final byte[] msgBytes = {
            0x20, 0x08, 0x00, 0x18,
            /* monitoring object */
            0x13, 0x10, 0x00, 0x0C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x64,
            /* pce-id object */
            0x19, 0x10, 0x00, 0x08, 0x7f, 0x00, 0x00, 0x02
        };
        ByteBuf result = Unpooled.wrappedBuffer(msgBytes);
        assertEquals(new PcmonreqBuilder().setPcreqMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
                result.readableBytes() - 4), Collections.emptyList()));
        ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcmonreqBuilder().setPcreqMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());

        result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCMonReq.bin"));
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message
            .pcreq.message.Requests> reqs2 = new ArrayList<>();
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message.pcreq
            .message.RequestsBuilder rBuilder1 =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep.types.rev181109.pcreq.message
                    .pcreq.message.RequestsBuilder();
        rBuilder1.setRp(rpTrue);
        final P2pBuilder p2pBuilder = new P2pBuilder()
                .setEndpointsObj(endpoints)
                .setMetrics(Lists.newArrayList(metrics))
                .setIro(iro);
        rBuilder1.setSegmentComputation(new SegmentComputationBuilder().setP2p(p2pBuilder.build()).build());
        reqs2.add(rBuilder1.build());
        builder.setRequests(reqs2)
                .setSvec(Lists.newArrayList(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pcep
                    .types.rev181109.pcreq.message.pcreq.message.SvecBuilder().setSvec(svec).build()));
        monReqBuilder.setMonitoring(monitoring).setPccIdReq(pccIdReq);
        final PceIdList pceIdList = new PceIdListBuilder().setPceId(pceId).build();
        monReqBuilder.setPceIdList(Lists.newArrayList(pceIdList, pceIdList));
        builder.setMonitoringRequest(monReqBuilder.build());

        assertEquals(new PcmonreqBuilder().setPcreqMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.emptyList()));
        buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcmonreqBuilder().setPcreqMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());
    }

    @Test
    public void testReplyMsgWithTwoEros() throws IOException, PCEPDeserializerException {
        // Success Reply with two EROs: the first one is followed by Bandwidth Object and one Metric Object

        final PCEPReplyMessageParser parser = new PCEPReplyMessageParser(objectRegistry);

        final PcrepMessageBuilder builder = new PcrepMessageBuilder();

        final List<Replies> replies = new ArrayList<>();

        final BandwidthBuilder bwBuilder = new BandwidthBuilder();
        bwBuilder.setIgnore(false);
        bwBuilder.setProcessingRule(false);
        bwBuilder.setBandwidth(new Bandwidth(new byte[] { (byte) 0x47, (byte) 0x74, (byte) 0x24, (byte) 0x00 }));

        RepliesBuilder repliesBuilder = new RepliesBuilder();
        repliesBuilder = new RepliesBuilder();
        repliesBuilder.setRp(rpTrue);
        final List<Paths> paths = new ArrayList<>();
        final PathsBuilder paBuilder1 = new PathsBuilder()
                .setEro(ero)
                .setBandwidth(bwBuilder.build())
                .setMetrics(Lists.newArrayList(metrics));
        paths.add(paBuilder1.build());
        final PathsBuilder paBuilder2 = new PathsBuilder();
        paBuilder2.setEro(ero);
        paths.add(paBuilder2.build());
        repliesBuilder.setResult(new SuccessCaseBuilder().setSuccess(new SuccessBuilder().setPaths(paths).build())
            .build()).build();
        replies.add(repliesBuilder.build());
        builder.setReplies(replies);

        ByteBuf result = Unpooled.wrappedBuffer(ByteArray.fileToBytes("src/test/resources/PCRep.7.bin"));
        assertEquals(new PcrepBuilder().setPcrepMessage(builder.build()).build(), parser.parseMessage(result.slice(4,
            result.readableBytes() - 4), Collections.emptyList()));
        ByteBuf buf = Unpooled.buffer(result.readableBytes());
        parser.serializeMessage(new PcrepBuilder().setPcrepMessage(builder.build()).build(), buf);
        assertArrayEquals(result.array(), buf.array());
    }
}
