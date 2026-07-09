#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import logging

import pytest

import controller_testlib.fixtures
from libraries import infra
from libraries.variables import variables

ODL_IP = variables.ODL_IP
TOOLS_IP = variables.TOOLS_IP
RESTCONF_PORT = variables.RESTCONF_PORT
KARAF_LOG_LEVEL = variables.KARAF_LOG_LEVEL
ODL_FEATRUES = [
    "odl-integration-compatible-with-all",
    "odl-infrautils-ready, odl-restconf-nb",
    "odl-jolokia",
    "odl-bgpcep-data-change-counter",
    "odl-bgpcep-bgp",
    "odl-bgpcep-bgp-config-example",
    "odl-bgpcep-pcep",
    "odl-bgpcep-bmp",
    "odl-bgpcep-bmp-config-example",
]

log = logging.getLogger(__name__)


def pytest_addoption(parser):
    """Adds custom command-line options to pytest."""
    parser.addoption(
        "--step-include",
        action="store",
        default=None,
        help="Comma-separated list of step tags to run",
    )
    parser.addoption(
        "--step-exclude",
        action="store",
        default=None,
        help="Comma-separated list of step tags to skip",
    )


allure_step_with_separate_logging = (
    controller_testlib.fixtures.allure_step_with_separate_logging
)
step_tag_checker = controller_testlib.fixtures.step_tag_checker
log_test_suite_start_end_to_karaf = (
    controller_testlib.fixtures.log_test_suite_start_end_to_karaf
)
log_test_case_start_end_to_karaf = (
    controller_testlib.fixtures.log_test_case_start_end_to_karaf
)
preconditions = controller_testlib.fixtures.make_preconditions_fixture(
    ODL_FEATRUES, karaf_log_level=KARAF_LOG_LEVEL
)


@pytest.fixture(scope="class")
def teardown_kill_all_running_play_script_processes():
    """Fixture to stop play.py script at the end of test class execution

    Args:
        None

    Returns:
        None
    """
    yield
    infra.shell(
        (
            r"pkill -f '^(.*/)?python3?\s+.*play.py' || "
            r"echo 'No running instance of play.py script.'"
        )
    )

@pytest.fixture(scope="class")
def teardown_kill_all_running_bgp_app_peer_script_processes():
    """Fixture to stop bgp_app_peer.py script at the end of test class execution

    Args:
        None

    Returns:
        None
    """
    yield
    infra.shell(
        (
            r"pkill -f '^(.*/)?python3?\s+.*bgp_app_peer.py' || "
            r"echo 'No running instance of bgp_app_peer.py script.'"
        )
    )

@pytest.fixture(scope="class")
def teardown_kill_all_running_exabgp_processes():
    """Fixture to stop exabgp instaces at the end of test class execution

    Args:
        None

    Returns:
        None
    """
    yield
    infra.shell(
        (
            r"pkill -f '^(.*/)?python3?\s+.*bin/exabgp' || "
            r"echo 'No running instance of play.py script.'"
        )
    )

@pytest.fixture(scope="class")
def teardown_kill_all_running_gobgp_processes():
    """Fixture to stop gobpd instaces at the end of test class execution

    Args:
        None

    Returns:
        None
    """
    yield
    infra.shell(
        (
            "pkill gobgpd || "
            "echo 'No running instance of gobgpd script.'"
        )
    )

@pytest.fixture(scope="class")
def teardown_kill_all_running_pcep_pcc_mock_processes():
    """Fixture to stop pcep-pcc-mock.jar instances at the end of test class execution

    Args:
        None

    Returns:
        None
    """
    yield
    infra.shell(
        (
            r"pkill -f '^(.*/)?java\s+.*pcep-pcc-mock\.jar' || "
            r"echo 'No running instance of pcep-pcc-mock.jar.'"
        )
    )
