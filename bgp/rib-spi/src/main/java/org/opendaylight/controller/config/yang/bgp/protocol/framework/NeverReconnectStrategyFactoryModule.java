package org.opendaylight.controller.config.yang.bgp.protocol.framework;

import io.netty.util.concurrent.EventExecutor;
import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.protocol.bgp.rib.protocol.NeverReconnectStrategy;
import org.opendaylight.protocol.bgp.rib.protocol.ReconnectStrategy;
import org.opendaylight.protocol.bgp.rib.protocol.ReconnectStrategyFactory;

public class NeverReconnectStrategyFactoryModule extends org.opendaylight.controller.config.yang.bgp.protocol.framework.AbstractNeverReconnectStrategyFactoryModule {
    public NeverReconnectStrategyFactoryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NeverReconnectStrategyFactoryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.bgp.protocol.framework.NeverReconnectStrategyFactoryModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        JmxAttributeValidationException.checkNotNull(getTimeout(), "value is not set.", timeoutJmxAttribute);
        JmxAttributeValidationException.checkCondition(getTimeout() >= 0, "value " + getTimeout() + " is less than 0",
            timeoutJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new NeverReconnectStrategyFactoryCloseable(getExecutorDependency(), getTimeout());
    }

    private static final class NeverReconnectStrategyFactoryCloseable implements ReconnectStrategyFactory, AutoCloseable {

        private final EventExecutor executor;
        private final int timeout;

        public NeverReconnectStrategyFactoryCloseable(final EventExecutor executor, final int timeout) {
            this.executor = executor;
            this.timeout = timeout;
        }

        @Override
        public void close() throws Exception {
            // no-op
        }

        @Override
        public ReconnectStrategy createReconnectStrategy() {
            return new NeverReconnectStrategy(this.executor, this.timeout);
        }

    }
}
