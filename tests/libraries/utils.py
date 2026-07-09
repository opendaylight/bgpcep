#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

from collections.abc import Callable
import difflib
import logging
import os
import time
from typing import Any

from jinja2 import Environment, FileSystemLoader
import controller_testlib.utils

from libraries import infra

log = logging.getLogger(__name__)

# Re-exported so existing "utils.deferred_logging(...)" etc. call sites keep
# working, but the origin of each is explicit here rather than hidden behind a
# bare-name import.
deferred_logging = controller_testlib.utils.deferred_logging
truncate_long_text = controller_testlib.utils.truncate_long_text
verify_jsons_match = controller_testlib.utils.verify_jsons_match
wait_until_function_pass = controller_testlib.utils.wait_until_function_pass
wait_until_function_returns_value = controller_testlib.utils.wait_until_function_returns_value
wait_until_function_returns_value_with_custom_value_validator = (
    controller_testlib.utils.wait_until_function_returns_value_with_custom_value_validator
)


def render_jinja_template(template_path: str, mapping: dict, filters: dict = None):
    file_dir, file_name = os.path.split(template_path)
    env = Environment(loader=FileSystemLoader(file_dir))
    if filters:
        env.filters.update(filters)
    template = env.get_template(file_name)

    return template.render(mapping)


def verify_multiline_text_match(expected_text: str, real_text: str):
    """Verify if multiline real text match expected text.

    Args:
        expected_text (str): Expected text.
        real_text (str): Real text to be verified.

    Returns:
        None
    """
    if expected_text != real_text:
        visual_diff = "\n".join(
            difflib.unified_diff(
                expected_text.splitlines(),
                real_text.splitlines(),
                fromfile="expected_text",
                tofile="real_text",
                lineterm="",
            )
        )
        raise AssertionError(f"Expected and real text does not match:\n{visual_diff}")


def verify_function_never_passes_within_timeout(
    retry_count: int, interval: int, function: Callable, *args, **kwargs
):
    """Verify that function call always raises excpetion within specific time
    interval.

    Args:
        retry_count (str): Total repetition count.
        interval (str): Interval in seconds between each verification.
        function (Callable): Function to be called.
        *args: Function positional arguments.
        **kwargs: Function keyword arguments.

    Returns:
        None
    """
    try:
        wait_until_function_pass(retry_count, interval, function, *args, **kwargs)
    except AssertionError:
        return
    except Exception as e:
        raise e
    else:
        raise AssertionError("Function did pass within timeout")


def verify_process_did_not_stop_immediately(
    pid: int, retry_count: int = 10, interval: int = 1
):
    """Verifies if just started process did not stop immediately

     This verification process is done by repeatedly checking the process status

     Args:
        pid (int): Process id.
        retry_count (int): Number of function call retries.
        interval (int): Interval in seconds between each verification.

    Returns:
        None
    """
    verify_function_returns_concrete_value_for_some_time(
        retry_count, interval, True, infra.is_process_still_running, pid
    )


def verify_function_does_not_fail_within_timeout(
    retry_count: int, interval: int, function: Callable, *args, **kwargs
) -> Any:
    """Retry provided funtion repeatedly if it never raises exception

    In order to pass provided function should not raise any exception.

    Args:
        retry_count (int): Maximum nuber of function calls retries.
        interval (int): Number of seconds to wait until next try.
        funtion (Callable): Function to be called, until it does not raise
            exception.
        *args: Function positional arguments.
        **kwargs: Function keyword arguments.

    Returns:
        Any: Return value returend by last successful function call.
    """
    validator = lambda value: True
    return (
        verify_function_returns_value_which_passes_custom_value_validator_for_some_time(
            retry_count, interval, validator, function, *args, **kwargs
        )
    )


def verify_function_returns_concrete_value_for_some_time(
    retry_count: int,
    interval: int,
    expected_value: Any,
    function: Callable,
    *args,
    **kwargs,
) -> Any:
    """Retry provided funtion repeatedly if it always return concrete value

    In order to pass provided function should not raise any exception.

    Args:
        retry_count (int): Total nuber of function calls retries.
        interval (int): Number of seconds to wait until next try.
        expected_value (Any): Value which is expected to be returned
            by the function call.
        funtion (Callable): Function to be called.
        *args: Function positional arguments.
        **kwargs: Function keyword arguments.

    Returns:
        Any: Return value returend by last successful function call.
    """
    validator = lambda value: value == expected_value
    return (
        verify_function_returns_value_which_passes_custom_value_validator_for_some_time(
            retry_count, interval, validator, function, *args, **kwargs
        )
    )


def verify_function_returns_value_which_passes_custom_value_validator_for_some_time(
    retry_count: int,
    interval: int,
    return_value_validator: Callable,
    function: Callable,
    *args,
    **kwargs,
) -> Any:
    """Retry provided function if it always passes value validator

    In order to pass provided function should not raise any exception.

    Args:
        retry_count (int): Total nuber of function calls.
        interval (int): Number of seconds to wait until next call.
        return_value_validator (Callable): Validator for evaluating
            returned value, if it is expected or not.
        function (Callable): Function to be called.
        *args: Function positional arguments.
        **kwargs: Function keyword arguments.

    Returns:
        Any: Return value returend by last successful function call.
    """
    logger_buffer = None

    for retry_num in range(retry_count):
        try:
            with deferred_logging() as logger_buffer:
                result = function(*args, **kwargs)
            passed_value_validator = return_value_validator(result)
        except Exception as e:
            logger_buffer.flush_to_target(log)
            raise AssertionError(
                f"Function {function.__name__}"
                f"({','.join([str(arg) for arg in args])} {kwargs or ''}) "
                f"failed with the following error {e}"
            )
        if not passed_value_validator:
            logger_buffer.flush_to_target(log)
            raise AssertionError(
                f"Function {function.__name__}"
                f"({','.join([str(arg) for arg in args])} {kwargs or ''}) "
                f"did not return expected value."
            )
        log.info(
            f"Function {function.__name__}"
            f"({','.join([str(arg) for arg in args])} {kwargs or ''}) "
            f"returned expected value ({retry_num}/{retry_count})"
        )
        time.sleep(interval)

    logger_buffer.flush_to_target(log)
    return result


def run_function_ignore_errors(function: Callable, *args, **kwargs):
    """Inovke function with provided arguments and ignore possible exceptions.

    Args:
        function (Callable): Function to be called.
        *args: Function positional arguments.
        **kwargs: Function keyword arguments.

    Returns:
        None
    """
    try:
        function(*args, **kwargs)
    except Exception as e:
        log.warning(
            f"Function {function.__name__}"
            f"({','.join([str(arg) for arg in args])} {kwargs or ''}) "
            f"with ignore errors failed on: \n{e}",
            exc_info=True,
        )
