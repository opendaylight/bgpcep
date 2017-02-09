package org.opendaylight.bgpcep.pcep.topology.provider;

import javax.annotation.Nullable;
import org.opendaylight.controller.config.yang.pcep.topology.provider.PCEPTopologyProviderRuntimeRegistrator;

public interface PCEPTopologyProviderRuntime {
    void registerRuntimeRootRegistration(@Nullable PCEPTopologyProviderRuntimeRegistrator runtimeRootRegistrator);
}
