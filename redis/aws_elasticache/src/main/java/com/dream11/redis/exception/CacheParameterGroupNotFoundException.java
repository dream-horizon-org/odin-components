package com.dream11.redis.exception;

import com.dream11.redis.error.ErrorCategory;

public class CacheParameterGroupNotFoundException extends RuntimeException {

  public CacheParameterGroupNotFoundException(String name) {
    super(String.format("%s: Cache Parameter Group:[%s] does not exists", ErrorCategory.ODIN_ERROR, name));
  }
}

