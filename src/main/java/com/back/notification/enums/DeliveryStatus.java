package com.back.notification.enums;

public enum DeliveryStatus {
    PENDING(false),
    PROCESSING(false),
    RETRY_WAITING(false),
    SUCCEEDED(true),
    FAILED(true);

    private final boolean terminal;

    DeliveryStatus(boolean terminal) {
        this.terminal = terminal;
    }

    public boolean isTerminal() {
        return terminal;
    }
}
