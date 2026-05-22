package com.back.notification.infrastructure.renderer;

import com.back.notification.domain.NotificationRequest;
import com.back.notification.enums.DeliveryChannel;
import com.back.notification.enums.NotificationType;
import com.back.notification.service.MessageRenderer;
import com.back.notification.service.RenderedMessage;
import org.springframework.stereotype.Component;

@Component
public class DefaultMessageRenderer implements MessageRenderer {

    @Override
    public RenderedMessage render(NotificationRequest request, DeliveryChannel channel) {
        NotificationType type = request.getNotificationType();
        return switch (type) {
            case ENROLLMENT_COMPLETED -> new RenderedMessage("수강 신청 완료", "수강 신청이 완료되었습니다.");
            case PAYMENT_CONFIRMED -> new RenderedMessage("결제 확정", "결제가 정상적으로 확정되었습니다.");
            case COURSE_STARTING_TOMORROW -> new RenderedMessage("강의 시작 알림", "내일 수강 예정인 강의가 시작됩니다.");
            case COURSE_CANCELED -> new RenderedMessage("강의 취소 알림", "신청하신 강의가 취소되었습니다.");
        };
    }
}
