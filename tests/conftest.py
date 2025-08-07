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
    #infra.shell("pkill -9 -f org.apache.karaf.main.[M]ain")
    rc, stdou = infra.shell("pgrep -f org.apache.karaf.main.Main | grep -v ^$$\$")
    log.warn(f"{rc=} {stdou=}")
    rc, stdou = infra.shell("pstree -p > /tmp/pstree")
    log.warn(f"{rc=} {stdou=}")
    infra.shell("kill $(pgrep -f org.apache.karaf.main.[M]ain | grep -v ^$$\$)")
    log.warn(f"{rc=} {stdou=}")

@pytest.fixture(scope="class")
def log_test_suite_start_end_to_karaf(preconditions, request):
    infra.log_message_to_karaf(f"Starting suite {request.cls.__name__}")
    yield
    infra.log_message_to_karaf(f"End of suite {request.cls.__name__}")

@pytest.fixture(scope="function")
def log_test_case_start_end_to_karaf(preconditions, request):
    infra.log_message_to_karaf(f"Starting test {request.cls.__name__}.{request.node.name}")
    yield
    infra.log_message_to_karaf(f"End of test {request.cls.__name__}.{request.node.name}")

@pytest.fixture(scope="class")
def teardown_kill_all_running_play_script_processes():
    yield
    infra.shell("kill $(pgrep -f play.py | grep -v ^$$\$) || echo 'No running instance of play.py script.'")