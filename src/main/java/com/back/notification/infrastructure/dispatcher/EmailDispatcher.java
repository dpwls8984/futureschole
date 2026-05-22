package com.back.notification.infrastructure.dispatcher;

import com.back.notification.domain.NotificationDelivery;
import com.back.notification.enums.DeliveryChannel;
import com.back.notification.service.ChannelDispatcher;
import com.back.notification.service.DispatchResult;
import com.back.notification.service.RenderedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailDispatcher implements ChannelDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatcher.class);

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.EMAIL;
    }

    @Override
    public DispatchResult dispatch(NotificationDelivery delivery, RenderedMessage message) {
        log.info(
                "[MOCK EMAIL SENT] recipient={}, notificationType={}, eventId={}, title={}, message={}",
                delivery.getNotificationRequest().getRecipientId(),
                delivery.getNotificationRequest().getNotificationType(),
                delivery.getNotificationRequest().getEventId(),
                message.title(),
                message.message()
        );
        return DispatchResult.success();
    }
}
