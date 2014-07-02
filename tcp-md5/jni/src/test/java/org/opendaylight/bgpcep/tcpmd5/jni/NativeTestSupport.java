package org.opendaylight.bgpcep.tcpmd5.jni;

import org.junit.Assume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NativeTestSupport {
    private static final Logger LOG = LoggerFactory.getLogger(NativeTestSupport.class);

    public static void assumeSupportedPlatform() {
        try {
            NativeKeyAccessFactory.getInstance();
        }catch (NativeSupportUnavailableException e) {
            LOG.info("Skipping test, this platform is not supported");
            Assume.assumeNoException(e);
        }
    }
}
