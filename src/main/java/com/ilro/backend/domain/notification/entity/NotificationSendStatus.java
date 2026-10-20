package com.ilro.backend.domain.notification.entity;

// 알림 발송 상태 - 실패 시 재시도 대상
public enum NotificationSendStatus {
    PENDING,
    SENT,
    FAILED
}
