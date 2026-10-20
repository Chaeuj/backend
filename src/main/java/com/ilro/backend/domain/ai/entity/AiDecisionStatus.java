package com.ilro.backend.domain.ai.entity;

// AI 호출 결과 - 실패 시 사장님 확인 전환·재처리 대상 (C09·O08)
public enum AiDecisionStatus {
    SUCCESS,
    FAILED,
    TIMEOUT
}
