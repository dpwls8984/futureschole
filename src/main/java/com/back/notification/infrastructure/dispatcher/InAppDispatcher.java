package com.back.notification.infrastructure.dispatcher;

import com.back.notification.domain.NotificationDelivery;
import com.back.notification.domain.NotificationInbox;
import com.back.notification.enums.DeliveryChannel;
import com.back.notification.infrastructure.persistence.NotificationInboxRepository;
import com.back.notification.service.ChannelDispatcher;
import com.back.notification.service.DispatchResult;
import com.back.notification.service.RenderedMessage;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class InAppDispatcher implements ChannelDispatcher {

    private final NotificationInboxRepository notificationInboxRepository;
    private final Clock clock;

    public InAppDispatcher(
            NotificationInboxRepository notificationInboxRepository,
            Clock clock
    ) {
        this.notificationInboxRepository = notificationInboxRepository;
        this.clock = clock;
    }

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.IN_APP;
    }

    @Override
    public DispatchResult dispatch(NotificationDelivery delivery, RenderedMessage message) {
        if (notificationInboxRepository.findByNotificationDeliveryId(delivery.getId()).isPresent()) {
            return DispatchResult.success();
        }

        NotificationInbox inbox = NotificationInbox.create(
                delivery,
                delivery.getNotificationRequest().getRecipientId(),
                message.title(),
                message.message(),
                LocalDateTime.now(clock)
        );
        notificationInboxRepository.save(inbox);
        return DispatchResult.success();
    }
}
