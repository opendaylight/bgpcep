package org.opendaylight.protocol.integration;

import com.google.common.collect.Lists;
import java.util.Collection;

public class TcpMd5NettyBundleTest extends AbstractBundleTest {

    @Override
    protected Collection<String> prerequisiteBundles() {
        return Lists.newArrayList("tcpmd5-api");
    }

    @Override
    protected Collection<String> requiredBundles() {
        return Lists.newArrayList("tcpmd5-netty");
    }

}
