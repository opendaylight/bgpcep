/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.openconfig.impl.util;

import java.util.List;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev150515.bgp.Global;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.RibImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.RibImplBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.bgp.rib.impl.rev130409.modules.module.configuration.rib.impl.LocalTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.Modules;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.Module;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.rev130405.modules.ModuleBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class ToConfigModuleUtil {

    public static final InstanceIdentifier<Modules> MODULES_IID = InstanceIdentifier.create(Modules.class);

    private ToConfigModuleUtil() {
        throw new UnsupportedOperationException();
    }

    public static boolean isSame(final RibImpl oldRibConf, final RibImpl newRibConf) {
        if (!oldRibConf.getBgpRibId().equals(newRibConf.getBgpRibId())) {
            return false;
        }
        if (!oldRibConf.getLocalAs().equals(newRibConf.getLocalAs())) {
            return false;
        }
        //do not care about the lists items order
        if (!(oldRibConf.getLocalTable().size() == newRibConf.getLocalTable().size() && oldRibConf.getLocalTable().containsAll(newRibConf.getLocalTable()))) {
            return false;
        }
        return true;
    }

    public static Module globalConfigToRibImplConfigModule(final Global globalConfig, final Module module, final List<LocalTable> tableTypes) {
        final RibImpl ribImpl = (RibImpl) module.getConfiguration();
        final RibImplBuilder ribImplBuilder = new RibImplBuilder();
        if (globalConfig.getConfig() != null) {
            ribImplBuilder.setBgpRibId(globalConfig.getConfig().getRouterId());
            ribImplBuilder.setLocalAs(globalConfig.getConfig().getAs().getValue());
        }
        ribImplBuilder.setLocalTable(tableTypes);
        ribImplBuilder.setBgpDispatcher(ribImpl.getBgpDispatcher());
        //TODO get cluster id from peer group
        ribImplBuilder.setClusterId(ribImpl.getClusterId());
        ribImplBuilder.setCodecTreeFactory(ribImpl.getCodecTreeFactory());
        ribImplBuilder.setDataProvider(ribImpl.getDataProvider());
        ribImplBuilder.setDomDataProvider(ribImpl.getDomDataProvider());
        ribImplBuilder.setExtensions(ribImpl.getExtensions());
        ribImplBuilder.setRibId(ribImpl.getRibId());
        ribImplBuilder.setSessionReconnectStrategy(ribImpl.getSessionReconnectStrategy());
        ribImplBuilder.setTcpReconnectStrategy(ribImpl.getTcpReconnectStrategy());
        ribImplBuilder.setOpenconfigProvider(ribImpl.getOpenconfigProvider());
        final ModuleBuilder mBuilder = new ModuleBuilder(module);
        mBuilder.setConfiguration(ribImplBuilder.build());
        mBuilder.setState(null);
        return mBuilder.build();
    }

    public static String getModuleName(final String provider) {
        return provider.substring(provider.lastIndexOf("=") + 2, provider.length() - 2);
    }

}
