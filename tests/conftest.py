#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import logging
import pytest

from lib import infra

ODL_IP = "127.0.0.1"
RESTCONF_PORT = "8181"
ODL_FEATRUES = ["odl-integration-compatible-with-all",
                "odl-infrautils-ready, odl-restconf-all",
                "odl-restconf-nb-rfc8040, odl-jolokia",
                "odl-bgpcep-data-change-counter",
                "odl-bgpcep-bgp",
                "odl-bgpcep-bgp-config-example"]

log = logging.getLogger(__name__)

@pytest.fixture(scope="class")
def preconditions():
    infra.shell("rm -rf tmp && mkdir tmp")
    infra.start_odl_with_features(ODL_FEATRUES, timeout=80)
    infra.execute_karaf_command("log:set INFO")
    yield
    infra.shell("pkill -9 -f org.apache.karaf.main.[M]ain")