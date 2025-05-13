package com.omnixys.invoice.exception;

import graphql.ErrorClassification;

public enum CustomErrorType implements ErrorClassification {
    PRECONDITION_FAILED,
    CONFLICT
}
