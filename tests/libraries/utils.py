#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

from collections.abc import Callable
import time
from typing import Any
import logging

from libraries import infra

log = logging.getLogger(__name__)


def wait_until_function_pass(
    retry_count: int, interval: int, function: Callable, *args, **kwargs
) -> Any:
    """Retry provided funtion with its argumetns repeatedly until it passes.

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
    return wait_until_function_returns_value_with_custom_value_validator(
        retry_count, interval, validator, function, *args, **kwargs
    )


def wait_until_function_returns_value(
    retry_count: int,
    interval: int,
    expected_value: Any,
    function: Callable,
    *args,
    **kwargs,
) -> Any:
    """Retry provided funtion repeatedly until it returns concrete value.

    In order to pass provided function should not raise any exception.

    Args:
        retry_count (int): Maximum nuber of function calls retries.
        interval (int): Number of seconds to wait until next try.
        expected_value (Any): Value which is expected to be returned
            by the function call.
        funtion (Callable): Function to be called, until it does not raise
            exception and returns expected value.
        *args: Function positional arguments.
        **kwargs: Function keyword arguments.

    Returns:
        Any: Return value returend by last successful function call.
    """
    validator = lambda value: value == expected_value
    return wait_until_function_returns_value_with_custom_value_validator(
        retry_count, interval, validator, function, *args, **kwargs
    )


def wait_until_function_returns_value_with_custom_value_validator(
    retry_count: int,
    interval: int,
    return_value_validator: Callable,
    function: Callable,
    *args,
    **kwargs,
) -> Any:
    """Retry provided funtion repeatedly until returns value passing validator.

    In order to pass provided function should not raise any exception.

    Args:
        retry_count (int): Maximum nuber of function calls retries.
        interval (int): Number of seconds to wait until next try.
        return_value_validator (Callable): Validator for evaluating
            returned value, if it is expected or not.
        funtion (Callable): Function to be called, until it does not raise
            exception and returns value passing validator call.
        *args: Function positional arguments.
        **kwargs: Function keyword arguments.

    Returns:
        Any: Return value returend by last successful function call.
    """
    for retry_num in range(retry_count):
        try:
            result = function(*args, **kwargs)
            if return_value_validator(result):
                return result
            else:
                raise AssertionError(
                    f"{function.__name__}({args} {kwargs or ''}) did not return "
                    f"expected value, but: {result}"
                )
        except Exception as e:
            last_exception = e
            log.info(
                f"{function.__name__}({args} {kwargs or ''}) failed with: {e} "
                f"({retry_num}/{retry_count})"
            )
        time.sleep(interval)

    raise AssertionError(
        f"Failed to execute {function.__name__}({','.join([str(arg) for arg in args])} {kwargs or ''}) "
        f"after {retry_count} attempts."
    ) from last_exception


def verify_process_did_not_stop_immediately(
    pid: int, retry_count: int = 10, interval: int = 1
):
    """Verifies if just started process did not stop immediately

     This verification process is done by repeatedly checking the process status

     Args:
        pid (int): Process id.
        retry_count (int): Number of repeated verif
        interval (int): Interval in seconds between each verification.

    Returns:
        None
    """
    verify_function_returns_concrete_value_for_some_time(
        retry_count, interval, True, infra.is_process_still_running, pid
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
        funtion (Callable): Function to be called.
        *args: Function positional arguments.
        **kwargs: Function keyword arguments.

    Returns:
        Any: Return value returend by last successful function call.
    """
    for retry_num in range(retry_count):
        try:
            result = function(*args, **kwargs)
            passed_value_validator = return_value_validator(result)
        except Exception as e:
            raise AssertionError(
                f"Function {function.__name__}({','.join([str(arg) for arg in args])} {kwargs or ''}) "
                f"failed with the following error {e}"
            )
        if not passed_value_validator:
            raise AssertionError(
                f"Function {function.__name__}({','.join([str(arg) for arg in args])} {kwargs or ''}) "
                f"did not return expected value."
            )
        log.info(
            f"Function {function.__name__}({','.join([str(arg) for arg in args])} {kwargs or ''}) returned expected value ({retry_num}/{retry_count})"
        )
        time.sleep(interval)

    return result
