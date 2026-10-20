package com.ilro.backend.domain.inquiry.entity;

import com.ilro.backend.domain.boiler.entity.BoilerErrorCode;
import com.ilro.backend.domain.customer.entity.Customer;
import com.ilro.backend.domain.owner.entity.Owner;
import com.ilro.backend.domain.owner.entity.OwnerWorkType;
import com.ilro.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

// 고객 문의(상담) 1건
@Getter
@Entity
@Table(name = "inquiry")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    // 초기에는 null 가능
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // 문의 접근 토큰의 SHA-256 해시 (원문은 DB에 저장하지 않음)
    @Column(nullable = false, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status = InquiryStatus.COLLECTING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryChannel channel = InquiryChannel.WEB;

    private String symptom;

    // 방문 주소 - 지도 API엔 이것만 사용 (C04)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(precision = 10, scale = 7)
    private BigDecimal lng;

    // 주차·출입 등 추가 안내 (C06)
    private String accessNote;

    // LLM 추출 현재값 - 출처·확인 여부는 InquiryField
    @Column(length = 50)
    private String manufacturer;

    @Column(length = 100)
    private String model;

    @Column(length = 50)
    private String boilerType;

    // 오류코드 원문
    @Column(length = 20)
    private String errorCode;

    // 오류코드 표 조회에 성공했을 때
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "error_code_id")
    private BoilerErrorCode boilerErrorCode;

    // 분류된 작업 유형
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_type_id")
    private OwnerWorkType workType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private HandlerMode handlerMode = HandlerMode.AI;

    // 낙관적 잠금
    @Version
    @Column(nullable = false)
    private Integer version;

    // true면 자동 예약·AI 재개 차단 (C09·B16)
    @Column(nullable = false)
    private boolean safetyFlag = false;

    // AI 요약 (B13), 생성 실패 시 null
    @Column(columnDefinition = "text")
    private String aiSummary;

    // 토큰 만료
    private Instant expiresAt;

    // 값이 있으면 목록·상세 조회 차단 (B12·O09)
    private Instant deletedAt;

    // 상세 주소 (동·호수)
    private String addressDetail;

    // 고객 전화번호 인증 시각, null이면 예약 확정 불가
    private Instant phoneVerifiedAt;

    @Builder
    private Inquiry(Owner owner, Customer customer, String tokenHash, Instant expiresAt) {
        this.owner = owner;
        this.customer = customer;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }
}
