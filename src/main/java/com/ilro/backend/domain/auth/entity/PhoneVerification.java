package com.ilro.backend.domain.auth.entity;

import com.ilro.backend.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

// 휴대전화 문자 인증 1회 - createdAt이 재발송 횟수 제한 기준
@Getter
@Entity
@Table(name = "phone_verification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneVerification extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationPurpose purpose;

    // 6자리 인증번호 해시 (원문 저장 금지)
    @Column(nullable = false)
    private String codeHash;

    // 입력 시도 횟수 - 반복 시도 제한
    @Column(nullable = false)
    private int attemptCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationSendStatus sendStatus;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant verifiedAt;

    @Builder
    private PhoneVerification(String phone, VerificationPurpose purpose, String codeHash,
                              VerificationSendStatus sendStatus, Instant expiresAt) {
        this.phone = phone;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.sendStatus = sendStatus;
        this.expiresAt = expiresAt;
    }
}
