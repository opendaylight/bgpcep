#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

from collections.abc import Callable
import difflib
import time
from typing import Any
import logging

from libraries import norm_json
from libraries import infra

log = logging.getLogger(__name__)


def verify_jsons_matach(json1: str, json2: str, json1_data_label: str = "json1",  json2_data_label: str = "json2", volatiles_list=()):
    normalized_json1 = norm_json.normalize_json_text(json1, keys_with_volatiles=volatiles_list)
    normalized_json2 = norm_json.normalize_json_text(json2, keys_with_volatiles=volatiles_list)
    log.debug(f"{normalized_json1=}")
    log.debug(f"{normalized_json2=}")

    if normalized_json1 != normalized_json2:
        visual_diff = "\n".join(
            difflib.unified_diff(
                normalized_json1.splitlines(),
                normalized_json2.splitlines(),
                fromfile=json1_data_label,
                tofile=json2_data_label,
                lineterm="",
                n = 2000
            )
        )
        if len(visual_diff) > 2000:
            visual_diff = visual_diff[:2000] + " ... (truncated long output)"
        raise AssertionError(f": \n{visual_diff}")
    


def verify_multiline_text_match(expected_text: str, real_text: str):
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
                    f"{function.__name__}({args} {kwargs or ''}) did not return " \
                    f"expected value, but: {result}"
                )
        except Exception as e:
            last_exception = e
            log.info(
                f"{function.__name__}({args} {kwargs or ''}) failed with: {e} "
                f"({retry_num}/{retry_count})"
            )
            log.debug(f"failed with: {e}")
        time.sleep(interval)

    raise AssertionError(
        f"Failed to execute {function.__name__}({','.join([str(arg) for arg in args])} {kwargs or ''}) " \
        f"after {retry_count} attempts."
    ) from last_exception
    
def verify_function_never_passes_within_timeout(
    retry_count: int, interval: int, function: Callable, *args, **kwargs
) -> Any:
    """????
    """
    try:
        wait_until_function_pass(
        retry_count, interval, function, *args, **kwargs
    )
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
        retry_count (int): Number of repeated verif
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
    return verify_function_returns_value_which_passes_custom_value_validator_for_some_time(
        retry_count, interval, validator, function, *args, **kwargs
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

def run_function_ignore_errors(function: Callable, *args, **kwargs):
    try:
        function(*args, **kwargs)
    except Exception as e:
        log.warning(f"Function {function.__name__}({','.join([str(arg) for arg in args])} {kwargs or ''}) " \
                f"with ignore errors failed on: \n{e}", exc_info=True)
