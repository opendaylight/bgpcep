/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.benchmark.app;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.protocol.bgp.parser.spi.PathIdUtil.NON_PATH_ID;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.Transaction;
import org.opendaylight.mdsal.binding.api.TransactionChain;
import org.opendaylight.mdsal.binding.api.TransactionChainListener;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.application.rib.tables.routes.Ipv4RoutesCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.bgp.rib.rib.loc.rib.tables.routes.Ipv4RoutesCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.Ipv4RoutesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev180329.ipv4.routes.ipv4.routes.Ipv4RouteKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.AsPathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.LocalPref;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.LocalPrefBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.MultiExitDisc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.MultiExitDiscBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.Origin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev200120.path.attributes.attributes.OriginBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.ApplicationRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.ApplicationRibBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.ApplicationRibId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.ApplicationRibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.BgpOrigin;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.Ipv4NextHopCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev200120.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev200120.AddPrefixInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev200120.AddPrefixOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev200120.AddPrefixOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev200120.DeletePrefixInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev200120.DeletePrefixOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev200120.DeletePrefixOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev200120.OdlBgpAppPeerBenchmarkService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev200120.output.Result;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odl.bgp.app.peer.benchmark.rev200120.output.ResultBuilder;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.util.BindingMap;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AppPeerBenchmark implements OdlBgpAppPeerBenchmarkService, TransactionChainListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AppPeerBenchmark.class);

    private static final AsPath AS_PATH = new AsPathBuilder().build();
    private static final Origin ORIGIN = new OriginBuilder().setValue(BgpOrigin.Igp).build();
    private static final MultiExitDisc MED = new MultiExitDiscBuilder().setMed(Uint32.ZERO).build();
    private static final LocalPref LOC_PREF = new LocalPrefBuilder().setPref(Uint32.valueOf(100)).build();
    private static final Map<TablesKey, Tables> EMPTY_TABLES = BindingMap.of(new TablesBuilder()
        .setAfi(Ipv4AddressFamily.VALUE).setSafi(UnicastSubsequentAddressFamily.VALUE).setRoutes(
            new Ipv4RoutesCaseBuilder().setIpv4Routes(new Ipv4RoutesBuilder().setIpv4Route(Map.of())
                .build()).build()).build());

    private static final String SLASH = "/";
    private static final String PREFIX = SLASH + "32";

    private final TransactionChain txChain;
    private final ObjectRegistration<OdlBgpAppPeerBenchmarkService> rpcRegistration;
    private final InstanceIdentifier<ApplicationRib> appIID;
    private final InstanceIdentifier<Ipv4Routes> routesIId;
    private final String appRibId;

    public AppPeerBenchmark(final DataBroker bindingDataBroker, final RpcProviderService rpcProviderRegistry,
            final String appRibId) {
        this.appRibId = requireNonNull(appRibId);
        txChain = bindingDataBroker.createMergingTransactionChain(this);

        appIID = InstanceIdentifier.builder(ApplicationRib.class,
            new ApplicationRibKey(new ApplicationRibId(appRibId))).build();
        routesIId = appIID
            .child(Tables.class, new TablesKey(Ipv4AddressFamily.VALUE, UnicastSubsequentAddressFamily.VALUE))
            .child(Ipv4RoutesCase.class, Ipv4Routes.class);
        rpcRegistration = rpcProviderRegistry.registerRpcImplementation(OdlBgpAppPeerBenchmarkService.class, this);
        LOG.info("BGP Application Peer Benchmark Application started.");
    }

    public void start() {
        LOG.debug("Instantiating App Peer Benchmark : {}", appRibId);
        final ApplicationRib appRib = new ApplicationRibBuilder().setId(new ApplicationRibId(
            new ApplicationRibId(appRibId))).setTables(EMPTY_TABLES).build();

        final WriteTransaction wTx = txChain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.CONFIGURATION, appIID, appRib);
        wTx.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.info("Empty Structure created for Application Peer Benchmark {}", appRibId);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Failed to create Empty Structure for Application Peer Benchmark {}", appRibId, throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public void onTransactionChainFailed(final TransactionChain chain, final Transaction transaction,
            final Throwable cause) {
        LOG.error("Broken chain {} in DatastoreBaAbstractWrite, transaction {}", chain, transaction.getIdentifier(),
            cause);
        close();
    }

    @Override
    public void onTransactionChainSuccessful(final TransactionChain chain) {
        LOG.debug("DatastoreBaAbstractWrite closed successfully, chain {}", chain);
    }

    @Override
    public ListenableFuture<RpcResult<AddPrefixOutput>> addPrefix(final AddPrefixInput input) {
        final long duration = addRoute(input.getPrefix(), input.getNexthop(), input.getCount(), input.getBatchsize());
        final long rate = countRate(duration, input.getCount());

        return RpcResultBuilder.success(
            new AddPrefixOutputBuilder().setResult(createResult(input.getCount(), duration, rate)).build())
            .buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<DeletePrefixOutput>> deletePrefix(final DeletePrefixInput input) {
        final long duration = deleteRoute(input.getPrefix(), input.getCount(), input.getBatchsize());
        final long rate = countRate(duration, input.getCount());

        return RpcResultBuilder.success(
            new DeletePrefixOutputBuilder().setResult(createResult(input.getCount(), duration, rate)).build())
            .buildFuture();
    }

    @Override
    public void close() {
        rpcRegistration.close();
        final WriteTransaction dTx = txChain.newWriteOnlyTransaction();
        dTx.delete(LogicalDatastoreType.CONFIGURATION, appIID);
        try {
            dTx.commit().get();
        } catch (final InterruptedException | ExecutionException e) {
            LOG.warn("Failed to clean-up BGP Application RIB.", e);
        }
        txChain.close();
        LOG.info("BGP Application Peer Benchmark Application closed.");
    }

    @VisibleForTesting
    InstanceIdentifier<Ipv4Routes> getIpv4RoutesIID() {
        return routesIId;
    }

    private long addRoute(final Ipv4Prefix ipv4Prefix, final Ipv4AddressNoZone nextHop, final Uint32 count,
            final Uint32 batch) {
        return processRoutes(ipv4Prefix, count, batch, new AttributesBuilder()
            .setCNextHop(new Ipv4NextHopCaseBuilder()
                .setIpv4NextHop(new Ipv4NextHopBuilder().setGlobal(nextHop).build())
                .build())
            .setMultiExitDisc(MED)
            .setLocalPref(LOC_PREF)
            .setOrigin(ORIGIN)
            .setAsPath(AS_PATH)
            .build());
    }

    private long deleteRoute(final Ipv4Prefix ipv4Prefix, final Uint32 count, final Uint32 batch) {
        return processRoutes(ipv4Prefix, count, batch, null);
    }

    private long processRoutes(final Ipv4Prefix ipv4Prefix, final Uint32 count, final Uint32 batch,
        final Attributes attributes) {
        WriteTransaction wt = txChain.newWriteOnlyTransaction();
        String address = getAdddressFromPrefix(ipv4Prefix);
        final long countLong = count.longValue();
        final long batchLong = batch.longValue();
        final Stopwatch stopwatch = Stopwatch.createStarted();
        for (int i = 1; i <= countLong; i++) {
            final Ipv4RouteKey routeKey = new Ipv4RouteKey(NON_PATH_ID, createKey(address));
            final KeyedInstanceIdentifier<Ipv4Route, Ipv4RouteKey> routeIId =
                routesIId.child(Ipv4Route.class, routeKey);
            if (attributes != null) {
                wt.put(LogicalDatastoreType.CONFIGURATION, routeIId, new Ipv4RouteBuilder()
                    .setRouteKey(routeKey.getRouteKey())
                    .setPrefix(new Ipv4Prefix(routeKey.getRouteKey()))
                    .withKey(routeKey)
                    .setAttributes(attributes)
                    .build());
            } else {
                wt.delete(LogicalDatastoreType.CONFIGURATION, routeIId);
            }
            if (i % batchLong == 0) {
                wt.commit().addCallback(new FutureCallback<CommitInfo>() {
                    @Override
                    public void onSuccess(final CommitInfo result) {
                        LOG.trace("Successful commit");
                    }

                    @Override
                    public void onFailure(final Throwable trw) {
                        LOG.error("Failed commit", trw);
                    }
                }, MoreExecutors.directExecutor());
                wt = txChain.newWriteOnlyTransaction();
            }
            address = increasePrefix(address);
        }
        wt.commit().addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                LOG.trace("Route batch stored.");
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Failed to store route batch.", throwable);
            }
        }, MoreExecutors.directExecutor());
        return stopwatch.stop().elapsed(TimeUnit.MILLISECONDS);
    }

    private static long countRate(final long durationMillis, final Uint32 count) {
        final long durationSec = TimeUnit.MILLISECONDS.toSeconds(durationMillis);
        if (durationSec != 0) {
            return count.toJava() / durationSec;
        }
        return count.toJava();
    }

    private static String increasePrefix(final String prefix) {
        return InetAddresses.increment(InetAddresses.forString(prefix)).getHostAddress();
    }

    private static Result createResult(final Uint32 count, final long duration, final long rate) {
        return new ResultBuilder().setCount(count).setDuration(Uint32.valueOf(duration)).setRate(Uint32.valueOf(rate))
                .build();
    }

    private static String getAdddressFromPrefix(final Ipv4Prefix prefix) {
        return prefix.getValue().split(SLASH)[0];
    }

    private static String createKey(final String address) {
        return address + PREFIX;
    }
}
