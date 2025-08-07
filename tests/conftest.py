#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import os
import logging
import pytest

from lib import infra

ODL_IP = os.environ["ODL_IP"]
RESTCONF_PORT = os.environ["RESTCONF_PORT"]
KARAF_LOG_LEVEL = os.environ["KARAF_LOG_LEVEL"]
ODL_FEATRUES = ["odl-integration-compatible-with-all",
                "odl-infrautils-ready, odl-restconf-all",
                "odl-restconf-nb-rfc8040, odl-jolokia",
                "odl-bgpcep-data-change-counter",
                "odl-bgpcep-bgp",
                "odl-bgpcep-bgp-config-example"]

log = logging.getLogger(__name__)

def pytest_sessionstart(session):
    """
    Called at the start of the test session to initialize a dictionary
    that will store the outcome of each test.
    """
    session.results = dict()

@pytest.hookimpl(tryfirst=True, hookwrapper=True)
def pytest_runtest_makereport(item, call):
    """
    Hook to capture the result of each test case. This runs after a test has finished.
    """
    # Execute all other hooks to obtain the report object
    outcome = yield
    report = outcome.get_result()

    if report.when == 'call':
        if item.session.results is not None:
            # Store the outcome ('passed', 'failed', etc.) using the test's unique ID
            item.session.results[item.nodeid] = report.outcome

def pytest_runtest_setup(item):
    """
    Hook to skip tests based on the custom 'skip_if_fails' marker.
    This version supports single or multiple dependencies.
    """
    marker = item.get_closest_marker("skip_if_fails")
    if not marker:
        return

    # Ensure dependencies are in a list, even if only one is provided
    dependency_names = marker.args[0]
    if not isinstance(dependency_names, list):
        dependency_names = [dependency_names]

    for dependency_name in dependency_names:
        dependency_nodeid = "::".join(item.nodeid.split("::")[:-1] + [dependency_name])

        results = item.session.results
        dependency_result = results.get(dependency_nodeid)

        # If any dependency did not pass, skip the test and stop checking
        if dependency_result != "passed":
            pytest.skip(f"Skipped because prerequisite test case '{dependency_name}' did not pass (outcome: {dependency_result}).")
            break

@pytest.fixture(scope="session")
def preconditions():
    infra.shell("rm -rf tmp && mkdir tmp")
    infra.start_odl_with_features(ODL_FEATRUES, timeout=80)
    infra.execute_karaf_command(f"log:set {KARAF_LOG_LEVEL}")
    yield
    infra.shell("kill $(pgrep -f org.apache.karaf.main.[M]ain | grep -v ^$$\$)")

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