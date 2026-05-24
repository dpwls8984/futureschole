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
            case PAYMENT_CANCELED -> new RenderedMessage("결제 취소 완료", "결제 취소 처리가 완료되었습니다.");
            case COURSE_STARTING_TOMORROW -> new RenderedMessage("강의 시작 알림", "내일 수강 예정인 강의가 시작됩니다.");
            case COMMENT_REPLIED -> new RenderedMessage("댓글 답글 알림", "작성하신 댓글에 답글이 등록되었습니다.");
            case LOGIN_FROM_NEW_DEVICE -> new RenderedMessage("새 기기 로그인 알림", "새로운 기기에서 로그인이 감지되었습니다.");
        };
    }
}
