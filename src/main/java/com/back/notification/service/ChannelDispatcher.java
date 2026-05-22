package com.back.notification.service;

import com.back.notification.domain.NotificationDelivery;
import com.back.notification.enums.DeliveryChannel;

public interface ChannelDispatcher {

    DeliveryChannel channel();

    DispatchResult dispatch(NotificationDelivery delivery, RenderedMessage message);
}
