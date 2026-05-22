package com.back.notification.enums;

import java.util.List;

public enum DispatchChannel {
    EMAIL(List.of(DeliveryChannel.EMAIL)),
    IN_APP(List.of(DeliveryChannel.IN_APP));

    private final List<DeliveryChannel> deliveryChannels;

    DispatchChannel(List<DeliveryChannel> deliveryChannels) {
        this.deliveryChannels = deliveryChannels;
    }

    public List<DeliveryChannel> deliveryChannels() {
        return deliveryChannels;
    }
}
