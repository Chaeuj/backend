package com.ilro.backend.schema;

import com.ilro.backend.domain.admin.entity.Admin;
import com.ilro.backend.domain.ai.entity.AiAction;
import com.ilro.backend.domain.ai.entity.AiDecision;
import com.ilro.backend.domain.ai.entity.AiDecisionStatus;
import com.ilro.backend.domain.ai.entity.AiStage;
import com.ilro.backend.domain.ai.entity.DecidedBy;
import com.ilro.backend.domain.auth.entity.PhoneVerification;
import com.ilro.backend.domain.auth.entity.VerificationPurpose;
import com.ilro.backend.domain.auth.entity.VerificationSendStatus;
import com.ilro.backend.domain.boiler.entity.BoilerErrorCode;
import com.ilro.backend.domain.customer.entity.Customer;
import com.ilro.backend.domain.inquiry.entity.Consent;
import com.ilro.backend.domain.inquiry.entity.ConsentType;
import com.ilro.backend.domain.inquiry.entity.HandoffRequest;
import com.ilro.backend.domain.inquiry.entity.HandoffRequestType;
import com.ilro.backend.domain.inquiry.entity.HandoffStatus;
import com.ilro.backend.domain.inquiry.entity.HandlerMode;
import com.ilro.backend.domain.inquiry.entity.Inquiry;
import com.ilro.backend.domain.inquiry.entity.InquiryField;
import com.ilro.backend.domain.inquiry.entity.InquiryFieldSource;
import com.ilro.backend.domain.inquiry.entity.InquiryFieldStatus;
import com.ilro.backend.domain.inquiry.entity.InquiryStatus;
import com.ilro.backend.domain.inquiry.entity.Message;
import com.ilro.backend.domain.inquiry.entity.MessageDeliveryStatus;
import com.ilro.backend.domain.inquiry.entity.MessageSender;
import com.ilro.backend.domain.inquiry.entity.Photo;
import com.ilro.backend.domain.inquiry.entity.PhotoUploadStatus;
import com.ilro.backend.domain.notification.entity.Notification;
import com.ilro.backend.domain.notification.entity.NotificationChannel;
import com.ilro.backend.domain.notification.entity.NotificationEventType;
import com.ilro.backend.domain.notification.entity.NotificationRecipient;
import com.ilro.backend.domain.notification.entity.NotificationRefType;
import com.ilro.backend.domain.notification.entity.NotificationSendStatus;
import com.ilro.backend.domain.notification.entity.PushSubscription;
import com.ilro.backend.domain.owner.entity.ApprovalStatus;
import com.ilro.backend.domain.owner.entity.Owner;
import com.ilro.backend.domain.owner.entity.OwnerBlock;
import com.ilro.backend.domain.owner.entity.OwnerBusinessHour;
import com.ilro.backend.domain.owner.entity.OwnerServiceArea;
import com.ilro.backend.domain.owner.entity.OwnerStatus;
import com.ilro.backend.domain.owner.entity.OwnerWorkType;
import com.ilro.backend.domain.owner.entity.WorkCategory;
import com.ilro.backend.domain.reservation.entity.ChangedBy;
import com.ilro.backend.domain.reservation.entity.RequestedBy;
import com.ilro.backend.domain.reservation.entity.Reservation;
import com.ilro.backend.domain.reservation.entity.ReservationChange;
import com.ilro.backend.domain.reservation.entity.ReservationChangeStatus;
import com.ilro.backend.domain.reservation.entity.ReservationChangeType;
import com.ilro.backend.domain.reservation.entity.ReservationHistory;
import com.ilro.backend.domain.reservation.entity.ReservationStatus;
import com.ilro.backend.domain.reservation.entity.WorkNote;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// Entity로 저장·조회했을 때 V1 테이블과 맞는지 확인 (ddl-auto: validate는 칸 이름·타입만 검사)
@SpringBootTest
@Transactional
class EntityMappingTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("V1의 모든 테이블에 Entity가 하나씩 있다")
    void everyTableHasEntity() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' "
                        + "AND table_name <> 'flyway_schema_history'",
                String.class);

        List<String> entityTables = em.getMetamodel().getEntities().stream()
                .map(entity -> entity.getJavaType().getAnnotation(Table.class).name())
                .collect(Collectors.toList());

        assertThat(entityTables).hasSize(22).containsExactlyInAnyOrderElementsOf(tables);
    }

    @Test
    @DisplayName("22개 Entity를 저장하면 DB 제약을 통과하고, 다시 읽으면 기본값·enum·JSON·요일이 그대로 나온다")
    void saveAndLoadAllEntities() {
        Instant start = Instant.parse("2099-01-05T01:00:00Z");
        Instant end = start.plus(1, ChronoUnit.HOURS);

        Admin admin = persist(Admin.builder().name("운영자").phone(unique("a")).build());
        Owner owner = persist(Owner.builder()
                .businessName("일로보일러").ownerName("김사장").phone(unique("o")).linkToken(unique("link")).build());
        OwnerBusinessHour monday = persist(OwnerBusinessHour.builder()
                .owner(owner).dayOfWeek(DayOfWeek.MONDAY).openTime(LocalTime.of(9, 0)).closeTime(LocalTime.of(18, 0))
                .build());
        persist(OwnerBusinessHour.builder().owner(owner).dayOfWeek(DayOfWeek.SUNDAY).isClosed(true).build());
        persist(OwnerServiceArea.builder().owner(owner).regionCode("41281").regionName("고양시 덕양구").build());
        OwnerWorkType repair = persist(OwnerWorkType.builder()
                .owner(owner).workCategory(WorkCategory.REPAIR).durationMinutes(60).build());
        persist(OwnerBlock.builder().owner(owner).startAt(start.minus(3, ChronoUnit.HOURS)).endAt(start).reason("개인 일정")
                .build());
        BoilerErrorCode errorCode = persist(BoilerErrorCode.builder()
                .manufacturer("경동나비엔").code(unique("E")).meaning("점화 불량").workCategory(WorkCategory.REPAIR).build());

        Customer customer = persist(Customer.builder().owner(owner).name("박고객").phone(unique("c")).build());
        Inquiry inquiry = persist(Inquiry.builder()
                .owner(owner).customer(customer).tokenHash(unique("token")).expiresAt(end).build());
        Consent consent = persist(Consent.builder()
                .inquiry(inquiry).consentType(ConsentType.PRIVACY_REQUIRED).termsVersion("v1").build());
        Message question = persist(Message.builder()
                .inquiry(inquiry).sender(MessageSender.CUSTOMER).content("보일러가 안 켜져요").clientMsgId(unique("m"))
                .build());
        Photo photo = persist(Photo.builder()
                .inquiry(inquiry).message(question).s3KeyOriginal("inquiry/1/original.jpg").mimeType("image/jpeg")
                .sizeBytes(1024).build());
        AiDecision decision = persist(AiDecision.builder()
                .inquiry(inquiry).message(question).stage(AiStage.DECIDE).modelName("claude-haiku-4-5")
                .inputSnapshot("{\"turn\": 1}").outputRaw("{\"action\": \"ASK_MORE_INFO\"}")
                .confidence(new BigDecimal("0.85")).action(AiAction.ASK_MORE_INFO).decidedBy(DecidedBy.EXTERNAL_API)
                .status(AiDecisionStatus.SUCCESS).build());
        persist(Message.builder()
                .inquiry(inquiry).sender(MessageSender.AI).aiDecision(decision).content("모델명을 알려 주세요").build());
        persist(InquiryField.builder()
                .inquiry(inquiry).fieldKey("manufacturer").fieldValue("경동나비엔").source(InquiryFieldSource.PHOTO)
                .status(InquiryFieldStatus.CONFIRMED).photo(photo).aiDecision(decision).build());
        HandoffRequest handoff = persist(HandoffRequest.builder()
                .inquiry(inquiry).requestType(HandoffRequestType.CALLBACK).name("박고객").phone("010-1234-5678").build());

        Reservation reservation = persist(Reservation.builder()
                .inquiry(inquiry).owner(owner).customer(customer).startAt(start).endAt(end).workType(repair)
                .travelTimeMinutes(20).workMinutes(60).bufferMinutes(15).build());
        ReservationChange change = persist(ReservationChange.builder()
                .reservation(reservation).changeType(ReservationChangeType.CHANGE).requestedBy(RequestedBy.CUSTOMER)
                .requestNote("다음 주 오전").prevStartAt(start).prevEndAt(end).baseVersion(0).build());
        persist(ReservationHistory.builder()
                .reservation(reservation).toStatus(ReservationStatus.REQUESTED).changedBy(ChangedBy.SYSTEM).build());
        persist(WorkNote.builder().reservation(reservation).content("배기통 점검 필요").build());

        Notification notification = persist(Notification.builder()
                .owner(owner).recipient(NotificationRecipient.OWNER).eventType(NotificationEventType.NEW_RESERVATION)
                .refType(NotificationRefType.RESERVATION).refId(reservation.getId()).channel(NotificationChannel.IN_APP)
                .dedupeKey(unique("dedupe")).build());
        persist(PushSubscription.builder()
                .owner(owner).endpoint("https://push.example.com/" + unique("ep")).p256dh("key").auth("secret").build());
        PhoneVerification verification = persist(PhoneVerification.builder()
                .phone(owner.getPhone()).purpose(VerificationPurpose.OWNER_LOGIN).codeHash("hash")
                .sendStatus(VerificationSendStatus.SENT).expiresAt(Instant.now().plus(3, ChronoUnit.MINUTES)).build());

        em.flush();
        em.clear();

        Owner foundOwner = em.find(Owner.class, owner.getId());
        assertThat(foundOwner.getStatus()).isEqualTo(OwnerStatus.READY);
        assertThat(foundOwner.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(foundOwner.getBufferMinutes()).isEqualTo(15);
        assertThat(foundOwner.getCreatedAt()).isNotNull();
        assertThat(foundOwner.getUpdatedAt()).isNotNull();

        assertThat(em.find(OwnerBusinessHour.class, monday.getId()).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(jdbc.queryForObject("SELECT day_of_week FROM owner_business_hour WHERE id = ?", Integer.class,
                monday.getId())).isEqualTo(1);

        Inquiry foundInquiry = em.find(Inquiry.class, inquiry.getId());
        assertThat(foundInquiry.getStatus()).isEqualTo(InquiryStatus.COLLECTING);
        assertThat(foundInquiry.getHandlerMode()).isEqualTo(HandlerMode.AI);
        assertThat(foundInquiry.getVersion()).isZero();

        AiDecision foundDecision = em.find(AiDecision.class, decision.getId());
        assertThat(foundDecision.getInputSnapshot()).isEqualTo("{\"turn\": 1}");
        assertThat(foundDecision.getConfidence()).isEqualByComparingTo("0.85");
        assertThat(jdbc.queryForObject("SELECT output_raw ->> 'action' FROM ai_decision WHERE id = ?", String.class,
                decision.getId())).isEqualTo("ASK_MORE_INFO");

        assertThat(em.find(Message.class, question.getId()).getDeliveryStatus()).isEqualTo(MessageDeliveryStatus.PENDING);
        assertThat(em.find(Photo.class, photo.getId()).getUploadStatus()).isEqualTo(PhotoUploadStatus.UPLOADING);
        assertThat(em.find(Consent.class, consent.getId()).getAgreedAt()).isNotNull();
        assertThat(em.find(HandoffRequest.class, handoff.getId()).getStatus()).isEqualTo(HandoffStatus.WAITING);
        assertThat(em.find(BoilerErrorCode.class, errorCode.getId()).getWorkCategory()).isEqualTo(WorkCategory.REPAIR);
        assertThat(em.find(OwnerWorkType.class, repair.getId()).isActive()).isTrue();

        Reservation foundReservation = em.find(Reservation.class, reservation.getId());
        assertThat(foundReservation.getStatus()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(foundReservation.getVersion()).isZero();
        assertThat(foundReservation.getStartAt()).isEqualTo(start);
        assertThat(em.find(ReservationChange.class, change.getId()).getStatus())
                .isEqualTo(ReservationChangeStatus.REQUESTED);

        assertThat(em.find(Notification.class, notification.getId()).getSendStatus())
                .isEqualTo(NotificationSendStatus.PENDING);
        assertThat(em.find(PhoneVerification.class, verification.getId()).getAttemptCount()).isZero();
        assertThat(em.find(Admin.class, admin.getId()).getRole().name()).isEqualTo("STAFF");
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }

    // UNIQUE 칸이 로컬 DB에 남아 있는 데이터와 겹치지 않게 임의 값 사용
    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
