package org.opendaylight.protocol.bmp.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import java.net.InetSocketAddress;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bmp.api.BmpDispatcher;
import org.opendaylight.protocol.bmp.impl.spi.BmpMonitor;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.tcpmd5.api.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.MonitorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.bmp.monitor.Monitor;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class BmpMonitorImpl implements BmpMonitor {

    private static final QName MONITOR_ID_QNAME = QName.cachedReference(QName.create(Monitor.QNAME, "id"));

    // check input parameters
    // create Session listener factory?
    // create empty monitor container in DS -> check if does not exist
    // dispatch server
    // create server session manager

    private final DOMDataBroker domDataBroker;
    private final YangInstanceIdentifier yangMonitorId;
    private final RouterSessionManager sessionManager;
    private final Channel channel;

    private BmpMonitorImpl(final DOMDataBroker domDataBroker, final YangInstanceIdentifier yangMonitorId,
            final Channel channel, final RouterSessionManager sessionManager) {
        this.domDataBroker = domDataBroker;
        this.yangMonitorId = yangMonitorId;
        this.channel = channel;
        this.sessionManager = sessionManager;
    }

    public static BmpMonitor createBmpMonitorInstance(final RIBExtensionConsumerContext ribExtensions, final BmpDispatcher dispatcher, final ReconnectStrategyFactory tcpStrategyFactory,
            final ReconnectStrategyFactory sessionStrategyFactory, final DOMDataBroker domDataBroker, final MonitorId monitorId, final InetSocketAddress address,
            final KeyMapping keys) {
        final YangInstanceIdentifier yangMonitorId = YangInstanceIdentifier.builder()
                .node(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bmp.monitor.rev150512.BmpMonitor.QNAME)
                .node(Monitor.QNAME)
                .nodeWithKey(Monitor.QNAME, MONITOR_ID_QNAME, monitorId.getValue())
                .build();
        final RouterSessionManager sessionManager = new RouterSessionManager(yangMonitorId, domDataBroker);
        final ChannelFuture channelFuture = dispatcher.createServer(address, keys, sessionManager);
        return new BmpMonitorImpl(domDataBroker, yangMonitorId, channelFuture.channel(), sessionManager);
    }

    @Override
    public void close() throws Exception {
        // TODO Auto-generated method stub
    }

}
