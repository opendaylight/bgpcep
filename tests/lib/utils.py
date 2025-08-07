#
# Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#

import datetime
import time
import logging

from lib import infra
from lib import templated_requests

log = logging.getLogger(__name__)

def retry_function(retry_count, interval, function, *args, **kwargs):
   validator = lambda value: True
   return retry_function_with_return_value_validator(retry_count, interval, function, validator, *args, **kwargs)

def retry_function_and_expect_value(retry_count, interval, expected_value, function, *args, **kwargs):
   validator = lambda value: value == expected_value
   return retry_function_with_return_value_validator(retry_count, interval, function, validator, *args, **kwargs)
    
def retry_function_with_return_value_validator(retry_count, interval, function, return_value_validator, *args, **kwargs):
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