package org.opendaylight.controller.config.yang.bgp.rib.impl;

import com.google.common.collect.Lists;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.protocol.bgp.rib.impl.server.BGPServerSessionValidator;

/**
* BGP peer acceptor that handles incomming bgp connections.
*/
public class BGPPeerAcceptorModule extends org.opendaylight.controller.config.yang.bgp.rib.impl.AbstractBGPPeerAcceptorModule {
    public BGPPeerAcceptorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BGPPeerAcceptorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerAcceptorModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // Try to parse address
        try {
            getAddress();
        } catch (final IllegalArgumentException e) {
            throw new JmxAttributeValidationException("Unable to resolve configured address", e, Lists.newArrayList(bindingAddressJmxAttribute, bindingPortJmxAttribute));
        }
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final ChannelFuture future = getBgpDispatcherDependency().createServer(getPeerRegistryDependency(), getAddress(), new BGPServerSessionValidator(getPeerRegistryDependency()));

        // Validate future success
        future.addListener(new GenericFutureListener<Future<Void>>() {
            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                if(future.isSuccess() == false) {
                    throw new IllegalStateException(String.format("Unable to start bgp server on %s", getAddress()), future.cause());
                }
            }
        });

        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                // TODO Does this shut the server down ?
                future.cancel(true);
                future.channel().close();
            }
        };
    }

    private InetSocketAddress getAddress() {
        final InetAddress inetAddr;
        try {
            inetAddr = InetAddress.getByName(getBindingAddress()
                    .getIpv4Address() != null ? getBindingAddress()
                    .getIpv4Address().getValue() : getBindingAddress()
                    .getIpv6Address().getValue());
        } catch (final UnknownHostException e) {
            throw new IllegalArgumentException("Illegal binding address " + getBindingAddress(), e);
        }
        return new InetSocketAddress(inetAddr, getBindingPort().getValue());
    }

}
