package com.ilro.backend.domain.notification.entity;

// 알림 사건 종류
public enum NotificationEventType {
    NEW_RESERVATION, // 새 예약
    NEEDS_CHECK, // 사장님 확인 필요
    CHANGE, // 변경
    CANCEL, // 취소
    RESULT // 처리 결과
}
