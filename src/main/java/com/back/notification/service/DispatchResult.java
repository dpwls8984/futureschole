package com.back.notification.service;

public sealed interface DispatchResult
        permits DispatchResult.Success, DispatchResult.RetryableFailure, DispatchResult.PermanentFailure {

    static DispatchResult success() {
        return new Success();
    }

    static DispatchResult retryableFailure(String code, String message) {
        return new RetryableFailure(code, message);
    }

    static DispatchResult permanentFailure(String code, String message) {
        return new PermanentFailure(code, message);
    }

    record Success() implements DispatchResult {
    }

    record RetryableFailure(String code, String message) implements DispatchResult {
    }

    record PermanentFailure(String code, String message) implements DispatchResult {
    }
}
