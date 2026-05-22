package com.back.notification.service;

import com.back.notification.domain.NotificationRequest;
import com.back.notification.enums.DeliveryChannel;

public interface MessageRenderer {

    RenderedMessage render(NotificationRequest request, DeliveryChannel channel);
}
