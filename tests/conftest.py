#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import allure
from collections.abc import Callable
from contextlib import contextmanager
import io
import logging
import os
import pytest
from typing import ContextManager, Generator, Iterator


from lib import infra

ODL_IP = os.environ["ODL_IP"]
RESTCONF_PORT = os.environ["RESTCONF_PORT"]
KARAF_LOG_LEVEL = os.environ["KARAF_LOG_LEVEL"]
ODL_FEATRUES = [
    "odl-integration-compatible-with-all",
    "odl-infrautils-ready, odl-restconf-all",
    "odl-restconf-nb-rfc8040, odl-jolokia",
    "odl-bgpcep-data-change-counter",
    "odl-bgpcep-bgp",
    "odl-bgpcep-bgp-config-example",
]

log = logging.getLogger(__name__)


@pytest.fixture
def allure_step_with_separate_logging(
    request: pytest.FixtureRequest,
) -> Callable[[str], ContextManager[None]]:
    """Provide context manager for Allure steps which separates logging

    This fixture extends standart allure_step context manger with functionality
    to store logs for each step seperately.

    Args:
        request (FixtureRequest): Request fixture for accessing test context.

    Returns:
        Callable: context manager for allure step with seperate logging.
    """

    @contextmanager
    def _log_step(title: str) -> Generator[any, None, None]:
        """Exectue allure step with seperate logging

        Args:
            title (str): Step title.

        Returns:
            Generator[any, None, None]: context manager for allure step
        """
        log_capture_string = io.StringIO()
        handler = logging.StreamHandler(log_capture_string)
        tox_ini_log_fromat = request.config.getini("log_format")
        formatter = logging.Formatter(tox_ini_log_fromat)
        handler.setFormatter(formatter)

        root_logger = logging.getLogger()
        root_logger.addHandler(handler)

        try:
            with allure.step(title) as allure_step:
                yield allure_step
        finally:
            root_logger.removeHandler(handler)
            log_contents = log_capture_string.getvalue()
            if log_contents:
                allure.attach(
                    log_contents,
                    name=f"Logs for '{title}'",
                    attachment_type=allure.attachment_type.TEXT,
                )

    return _log_step


@pytest.fixture(scope="session")
def preconditions():
    """Fixture for basic test session setup.

    It handles setting features to be installed, starting karaf, etc.

    Args:
        None

    Returns:
        None
    """
    infra.shell("rm -rf tmp && mkdir tmp")
    infra.shell("ls results || mkdir results")
    infra.start_odl_with_features(ODL_FEATRUES, timeout=80)
    infra.execute_karaf_command(f"log:set {KARAF_LOG_LEVEL}")
    yield
    infra.shell("kill $(pgrep -f org.apache.karaf.main.[M]ain | grep -v ^$$\$)")


@pytest.fixture(scope="class")
def log_test_suite_start_end_to_karaf(request: pytest.FixtureRequest):
    """Fixture to log in karaf test suite start and end markers

    Args:
        request (FixtureRequest): Request fixture for accessing test context.

    Returns:
        None
    """
    infra.log_message_to_karaf(f"Starting suite {request.cls.__name__}")
    yield
    infra.log_message_to_karaf(f"End of suite {request.cls.__name__}")


@pytest.fixture(scope="function")
def log_test_case_start_end_to_karaf(request: pytest.FixtureRequest):
    """Fixture to log in karaf test case start and end markers

    Args:
        request (FixtureRequest): Request fixture for accessing test context.

    Returns:
        None
    """
    infra.log_message_to_karaf(
        f"Starting test {request.cls.__name__}.{request.node.name}"
    )
    yield
    infra.log_message_to_karaf(
        f"End of test {request.cls.__name__}.{request.node.name}"
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
        "kill $(pgrep -f play.py | grep -v ^$$\$) || echo 'No running instance of play.py script.'"
    )
