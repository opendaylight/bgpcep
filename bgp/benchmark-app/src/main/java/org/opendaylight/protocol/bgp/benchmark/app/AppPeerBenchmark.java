/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.benchmark.app;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.application.rib.tables.routes.Ipv4RoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.Ipv4RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.ipv4.routes.Ipv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev171207.ipv4.routes.ipv4.routes.Ipv4RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.PathId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev171207.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.ApplicationRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.ApplicationRibBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.ApplicationRibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.AddPrefixInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.AddPrefixOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.AddPrefixOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.DeletePrefixInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.DeletePrefixOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.DeletePrefixOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.OdlBgpAppPeerBenchmarkService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.output.Result;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev160309.output.ResultBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppPeerBenchmark implements OdlBgpAppPeerBenchmarkService, TransactionChainListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AppPeerBenchmark.class);

    private static final AsPath AS_PATH = new AsPathBuilder().build();
    private static final Origin ORIGIN = new OriginBuilder().setValue(BgpOrigin.Igp).build();
    private static final MultiExitDisc MED = new MultiExitDiscBuilder().setMed(0L).build();
    private static final LocalPref LOC_PREF = new LocalPrefBuilder().setPref(100L).build();
    private static final List<Tables> EMPTY_TABLES = Collections.singletonList(new TablesBuilder()
        .setAfi(Ipv4AddressFamily.class).setSafi(UnicastSubsequentAddressFamily.class).setRoutes(
            new Ipv4RoutesCaseBuilder().setIpv4Routes(new Ipv4RoutesBuilder().setIpv4Route(Collections.emptyList())
                .build()).build()).build());

    private static final String SLASH = "/";
    private static final String PREFIX = SLASH + "32";

    private static final PathId PATH_ID = new PathId(0L);

    private final BindingTransactionChain txChain;
    private final RpcRegistration<OdlBgpAppPeerBenchmarkService> rpcRegistration;
    private final InstanceIdentifier<ApplicationRib> appIID;
    private final InstanceIdentifier<Ipv4Routes> routesIId;
    private final String appRibId;

    public AppPeerBenchmark(final DataBroker bindingDataBroker, final RpcProviderRegistry rpcProviderRegistry,
            final String appRibId) {
        this.appRibId = requireNonNull(appRibId);
        this.txChain = bindingDataBroker.createTransactionChain(this);

        this.appIID = InstanceIdentifier.builder(ApplicationRib.class,
            new ApplicationRibKey(new ApplicationRibId(appRibId))).build();
        final InstanceIdentifier tablesIId = this.appIID
                .child(Tables.class, new TablesKey(Ipv4AddressFamily.class, UnicastSubsequentAddressFamily.class));
        this.routesIId = tablesIId.child(Ipv4Routes.class);
        this.rpcRegistration = rpcProviderRegistry.addRpcImplementation(OdlBgpAppPeerBenchmarkService.class, this);
        LOG.info("BGP Application Peer Benchmark Application started.");
    }

    public void start() {
        LOG.debug("Instantiating App Peer Benchmark : {}", this.appRibId);
        final ApplicationRib appRib = new ApplicationRibBuilder().setId(new ApplicationRibId(
            new ApplicationRibId(this.appRibId))).setTables(EMPTY_TABLES).build();

        final WriteTransaction wTx = this.txChain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, this.appIID, appRib);
        Futures.addCallback(wTx.submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.info("Empty Structure created for Application Peer Benchmark {}", AppPeerBenchmark.this.appRibId);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Failed to create Empty Structure for Application Peer Benchmark {}",
                    AppPeerBenchmark.this.appRibId, throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain<?, ?> chain, final AsyncTransaction<?, ?> transaction,
            final Throwable cause) {
        LOG.error("Broken chain {} in DatastoreBaAbstractWrite, transaction {}, cause {}", chain,
                transaction.getIdentifier(), cause);
        close();
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
        LOG.debug("DatastoreBaAbstractWrite closed successfully, chain {}", chain);
    }

    @Override
    public Future<RpcResult<AddPrefixOutput>> addPrefix(final AddPrefixInput input) {
        final long duration = addRoute(input.getPrefix(), input.getNexthop(), input.getCount(), input.getBatchsize());
        final long rate = countRate(duration, input.getCount());

        final AddPrefixOutputBuilder outputbuilder = new AddPrefixOutputBuilder();
        outputbuilder.setResult(createResult(input.getCount(), duration, rate));
        final AddPrefixOutput output = outputbuilder.build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<DeletePrefixOutput>> deletePrefix(final DeletePrefixInput input) {
        final long duration = deleteRoute(input.getPrefix(), input.getCount(), input.getBatchsize());
        final long rate = countRate(duration, input.getCount());

        final DeletePrefixOutputBuilder outputbuilder = new DeletePrefixOutputBuilder();
        outputbuilder.setResult(createResult(input.getCount(), duration, rate));
        final DeletePrefixOutput output = outputbuilder.build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public void close() {
        this.rpcRegistration.close();
        final WriteTransaction dTx = this.txChain.newWriteOnlyTransaction();
        dTx.delete(LogicalDatastoreType.CONFIGURATION, this.appIID);
        try {
            dTx.submit().checkedGet();
        } catch (final TransactionCommitFailedException e) {
            LOG.warn("Failed to clean-up BGP Application RIB.", e);
        }
        this.txChain.close();
        LOG.info("BGP Application Peer Benchmark Application closed.");
    }

    @VisibleForTesting
    InstanceIdentifier<Ipv4Routes> getIpv4RoutesIID() {
        return this.routesIId;
    }

    private long addRoute(final Ipv4Prefix ipv4Prefix, final Ipv4Address nextHop, final long count, final long batch) {
        final AttributesBuilder attributesBuilder = new AttributesBuilder();
        attributesBuilder.setCNextHop(new Ipv4NextHopCaseBuilder().setIpv4NextHop(
                new Ipv4NextHopBuilder().setGlobal(new Ipv4Address(nextHop)).build()).build());
        attributesBuilder.setMultiExitDisc(MED);
        attributesBuilder.setLocalPref(LOC_PREF);
        attributesBuilder.setOrigin(ORIGIN);
        attributesBuilder.setAsPath(AS_PATH);
        final Attributes attributes = attributesBuilder.build();
        return processRoutes(ipv4Prefix, count, batch, attributes);
    }

    private long deleteRoute(final Ipv4Prefix ipv4Prefix, final long count, final long batch) {
        return processRoutes(ipv4Prefix, count, batch, null);
    }

    private long processRoutes(final Ipv4Prefix ipv4Prefix, final long count, final long batch,
        final Attributes attributes) {
        WriteTransaction wt = this.txChain.newWriteOnlyTransaction();
        String address = getAdddressFromPrefix(ipv4Prefix);
        final Stopwatch stopwatch = Stopwatch.createStarted();
        for (int i = 1; i <= count; i++) {
            final Ipv4RouteKey routeKey = new Ipv4RouteKey(PATH_ID, createPrefix(address));
            final KeyedInstanceIdentifier<Ipv4Route, Ipv4RouteKey> routeIId =
                this.routesIId.child(Ipv4Route.class, routeKey);
            if (attributes != null) {
                final Ipv4RouteBuilder ipv4RouteBuilder = new Ipv4RouteBuilder();
                ipv4RouteBuilder.setPrefix(routeKey.getPrefix());
                ipv4RouteBuilder.setKey(routeKey);
                ipv4RouteBuilder.setAttributes(attributes);
                final Ipv4Route ipv4Route = ipv4RouteBuilder.build();
                wt.put(LogicalDatastoreType.CONFIGURATION, routeIId,
                        ipv4Route);
            } else {
                wt.delete(LogicalDatastoreType.CONFIGURATION, routeIId);
            }
            if (i % batch == 0) {
                wt.submit();
                wt = this.txChain.newWriteOnlyTransaction();
            }
            address = increasePrefix(address);
        }
        wt.submit();
        return stopwatch.stop().elapsed(TimeUnit.MILLISECONDS);
    }

    private static long countRate(final long durationMillis, final long count) {
        final long durationSec = TimeUnit.MILLISECONDS.toSeconds(durationMillis);
        if (durationSec != 0) {
            return count / durationSec;
        }
        return count;
    }

    private static String increasePrefix(final String prefix) {
        return InetAddresses.increment(InetAddresses.forString(prefix)).getHostAddress();
    }

    private static Result createResult(final long count, final long duration, final long rate) {
        return new ResultBuilder().setCount(count).setDuration(duration).setRate(rate).build();
    }

    private static String getAdddressFromPrefix(final Ipv4Prefix prefix) {
        return prefix.getValue().split(SLASH)[0];
    }

    private static Ipv4Prefix createPrefix(final String address) {
        return new Ipv4Prefix(address + PREFIX);
    }
}