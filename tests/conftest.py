#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import allure
from contextlib import contextmanager
import io
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

@pytest.fixture
def step_logger():
    """A fixture that provides a log-capturing context manager for Allure steps."""

    @contextmanager
    def _log_step(title: str):
        log_capture_string = io.StringIO()
        handler = logging.StreamHandler(log_capture_string)
        formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
        handler.setFormatter(formatter)

        root_logger = logging.getLogger()
        root_logger.addHandler(handler)

        try:
            with allure.step(title):
                yield
        finally:
            root_logger.removeHandler(handler)
            log_contents = log_capture_string.getvalue()
            if log_contents:
                allure.attach(
                    log_contents,
                    name=f"Logs for '{title}'",
                    attachment_type=allure.attachment_type.TEXT
                )

    return _log_step

def pytest_runtest_setup(item):
    """Hook to skip tests based on the custom 'skip_if_fails' marker.
    This version supports single or multiple dependencies."""
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
    infra.shell("ls results || mkdir results")
    infra.start_odl_with_features(ODL_FEATRUES, timeout=80)
    infra.execute_karaf_command(f"log:set {KARAF_LOG_LEVEL}")
    yield
    infra.shell("kill $(pgrep -f org.apache.karaf.main.[M]ain | grep -v ^$$\$)")

@pytest.fixture(scope="class")
def log_test_suite_start_end_to_karaf(preconditions, request):
    """Fixture to log in karaf test suite start and end markers"""
    infra.log_message_to_karaf(f"Starting suite {request.cls.__name__}")
    yield
    infra.log_message_to_karaf(f"End of suite {request.cls.__name__}")

@pytest.fixture(scope="function")
def log_test_case_start_end_to_karaf(preconditions, request):
    """Fixture to log in karaf test case start and end markers"""
    infra.log_message_to_karaf(f"Starting test {request.cls.__name__}.{request.node.name}")
    yield
    infra.log_message_to_karaf(f"End of test {request.cls.__name__}.{request.node.name}")

@pytest.fixture(scope="class")
def teardown_kill_all_running_play_script_processes():
    """Fixture to stop play.py script at the end of test class execution"""
    yield
    infra.shell("kill $(pgrep -f play.py | grep -v ^$$\$) || echo 'No running instance of play.py script.'")