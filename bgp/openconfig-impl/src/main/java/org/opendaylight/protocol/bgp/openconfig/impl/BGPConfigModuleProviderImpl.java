/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl;

import static org.opendaylight.protocol.bgp.openconfig.impl.util.ToConfigModuleUtil.MODULES_IID;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.protocol.bgp.openconfig.impl.spi.BGPConfigModuleProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.Services;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.services.ServiceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class BGPConfigModuleProviderImpl implements BGPConfigModuleProvider {

    private static final InstanceIdentifier<Services> SERVICES_IID = InstanceIdentifier.create(Services.class);

    @Override
    public void writeModuleConfiguration(final Module module, final WriteTransaction wTx) {
        wTx.put(LogicalDatastoreType.CONFIGURATION, MODULES_IID.child(Module.class, module.getKey()), module);
        wTx.submit();
    }

    @Override
    public void removeModuleConfiguration(final ModuleKey moduleKey, final WriteTransaction wTx) {
        wTx.delete(LogicalDatastoreType.CONFIGURATION, MODULES_IID.child(Module.class, moduleKey));
        wTx.submit();
    }

    @Override
    public ListenableFuture<Optional<Module>> readModuleConfiguration(final ModuleKey moduleKey, final ReadTransaction rTx) {
        return rTx.read(LogicalDatastoreType.CONFIGURATION, MODULES_IID.child(Module.class, moduleKey));
    }

    @Override
    public ListenableFuture<Optional<Service>> readConfigService(final ServiceKey serviceKey, final ReadTransaction rTx) {
        return rTx.read(LogicalDatastoreType.CONFIGURATION, SERVICES_IID.child(Service.class, serviceKey));
    }

}
