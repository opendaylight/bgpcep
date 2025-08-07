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

log = logging.getLogger(__name__)

def retry_function(retry_count: int, interval: int, function: Callable, *args, **kwargs) -> Any:
   """Retry provided funtion with its argumetns repeatedly until it does not raise exception or times out.

    Args:
        retry_count (int): Maximum nuber of function calls retries.
        interval (int): Number of seconds to wait until next try.
        funtion (Callable): Function to be called, until it does not raise exception.
        *args: Function positional arguments.
        **kwargs: Function keyword arguments.

    Returns:
        Any: Return value returend by last successful function call.
    """
   validator = lambda value: True
   return retry_function_with_return_value_validator(retry_count, interval, validator, function, *args, **kwargs)

def retry_function_and_expect_value(retry_count: int, interval: int, expected_value: Any, function: Callable, *args, **kwargs) -> Any:
   """Retry provided funtion with its argumetns repeatedly until it does not raise exception and returns expected value or times out.

    Args:
        retry_count (int): Maximum nuber of function calls retries.
        interval (int): Number of seconds to wait until next try.
        expected_value (Any): Value which is expected to be returned by the function call.
        funtion (Callable): Function to be called, until it does not raise exception and returns expected value.
        *args: Function positional arguments.
        **kwargs: Function keyword arguments.

    Returns:
        Any: Return value returend by last successful function call.
    """
   validator = lambda value: value == expected_value
   return retry_function_with_return_value_validator(retry_count, interval, validator, function, *args, **kwargs)
    
def retry_function_with_return_value_validator(retry_count: int, interval: int, return_value_validator: Callable, function: Callable, *args, **kwargs) -> Any:
    """Retry provided funtion with its argumetns repeatedly until it does not raise exception and returns value passing validator call or times out.

    Args:
        retry_count (int): Maximum nuber of function calls retries.
        interval (int): Number of seconds to wait until next try.
        return_value_validator (Callable): Validator for evaluating returned value, if it is expected or not.
        funtion (Callable): Function to be called, until it does not raise exception and returns value passing validator call.
        *args: Function positional arguments.
        **kwargs: Function keyword arguments.

    Returns:
        Any: Return value returend by last successful function call.
    """
    for retry_num in range(retry_count):
        try:
            result = function(*args, **kwargs)
            if not return_value_validator(result):
                log.info(f"{function.__name__}({','.join(args)} {kwargs or ''}) did not return expected value, but: {result}")
            else:
                return result
        except Exception as e:
            log.info(f"{function.__name__}({','.join(args)} {kwargs or ''}) failed with: {e} ({retry_num}/{retry_count})")
        time.sleep(interval)
    else:
        raise Exception(f"Failed to execute {function.__name__}({','.join(args)} {kwargs or ''}) after {retry_count} attempts.")