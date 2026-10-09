package com.bankpaymentmonitor.paymentservice.payment.repository;

import com.bankpaymentmonitor.paymentservice.payment.dto.PaymentCreateDTO;
import com.bankpaymentmonitor.paymentservice.payment.service.PaymentOutboxPublisherRepository;
import com.bankpaymentmonitor.paymentservice.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PaymentOutboxPublisherRepositoryIntegrationTest {
    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private PaymentOutboxPublisherRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PaymentService paymentService;

    @Test
    void shouldMarkOutboxEventAsPublished() {

        var payment = paymentService.createPayment(
                new PaymentCreateDTO(
                        "TEST_SYSTEM",
                        UUID.randomUUID().toString(),
                        "BR-001",
                        new BigDecimal("150.00"),
                        "USD"
                )
        );

        UUID eventId = UUID.randomUUID();

        jdbcTemplate.update("""
            INSERT INTO payment_outbox
                (id, aggregate_id, event_type, payload, created_at)
            VALUES (?, ?, 'PAYMENT_CREATED', CAST(? AS jsonb), CURRENT_TIMESTAMP)
            """,
                eventId,
                payment.reference(),
                "{\"eventType\":\"PAYMENT_CREATED\"}"
        );

        repository.markAsPublished(eventId);

        Boolean published = jdbcTemplate.queryForObject("""
            SELECT published_at IS NOT NULL
            FROM payment_outbox
            WHERE id = ?
            """,
                Boolean.class,
                eventId
        );

        assertEquals(Boolean.TRUE, published);
    }
    @Test
    void shouldFindPendingOutboxEvents() {

        var payment = paymentService.createPayment(
                new PaymentCreateDTO(
                        "TEST_SYSTEM",
                        UUID.randomUUID().toString(),
                        "BR-001",
                        new BigDecimal("150.00"),
                        "USD"
                )
        );

        var pendingEvents = repository.findPendingEvents(100);

        assertTrue(
                pendingEvents.stream()
                        .anyMatch(event ->
                                event.paymentReference().equals(payment.reference())
                        )
        );
    }
    @Test
    void shouldNotReturnAlreadyPublishedEvents() {

        // Arrange : créer un paiement et son événement Outbox
        var payment = paymentService.createPayment(
                new PaymentCreateDTO(
                        "TEST_SYSTEM",
                        UUID.randomUUID().toString(),
                        "BR-001",
                        new BigDecimal("150.00"),
                        "USD"
                )
        );

        UUID eventId = jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM payment_outbox
                WHERE aggregate_id = ?
                """,
                UUID.class,
                payment.reference()
        );

        // Marquer l'événement comme publié
        repository.markAsPublished(eventId);

        // Act : récupérer les événements en attente
        var pendingEvents = repository.findPendingEvents(100);

        // Assert : l'événement publié ne doit pas apparaître
        assertFalse(
                pendingEvents.stream()
                        .anyMatch(event -> event.id().equals(eventId))
        );
    }
    @Test
    void shouldNotClaimSameOutboxEventTwice() {

        // Arrange : créer un paiement et son événement Outbox
        var payment = paymentService.createPayment(
                new PaymentCreateDTO(
                        "TEST_SYSTEM",
                        UUID.randomUUID().toString(),
                        "BR-001",
                        new BigDecimal("150.00"),
                        "USD"
                )
        );

        UUID workerA = UUID.randomUUID();
        UUID workerB = UUID.randomUUID();

        // Act : le premier worker réserve les événements
        var firstClaim = repository.claimPendingEvents(100, workerA);

        // Le deuxième worker tente de réserver les mêmes événements
        var secondClaim = repository.claimPendingEvents(100, workerB);

        // Assert : notre événement est réservé par le premier worker
        assertTrue(
                firstClaim.stream()
                        .anyMatch(event ->
                                event.paymentReference().equals(payment.reference())
                        )
        );

        // Le deuxième worker ne doit pas récupérer cet événement
        assertFalse(
                secondClaim.stream()
                        .anyMatch(event ->
                                event.paymentReference().equals(payment.reference())
                        )
        );
    }
    @Test
    void shouldReclaimOutboxEventAfterClaimExpiration() {

        // Arrange : créer un paiement
        var payment = paymentService.createPayment(
                new PaymentCreateDTO(
                        "TEST_SYSTEM",
                        UUID.randomUUID().toString(),
                        "BR-001",
                        new BigDecimal("150.00"),
                        "USD"
                )
        );

        UUID workerA = UUID.randomUUID();
        UUID workerB = UUID.randomUUID();

        // Le premier worker réserve l'événement
        var firstClaim = repository.claimPendingEvents(100, workerA);

        var claimedEvent = firstClaim.stream()
                .filter(event ->
                        event.paymentReference().equals(payment.reference())
                )
                .findFirst()
                .orElseThrow();

        // Simuler l'expiration de sa réservation
        jdbcTemplate.update(
                """
                UPDATE payment_outbox
                SET claimed_until = CURRENT_TIMESTAMP - INTERVAL '1 minute'
                WHERE id = ?
                """,
                claimedEvent.id()
        );

        // Act : un autre worker tente de récupérer l'événement
        var secondClaim = repository.claimPendingEvents(100, workerB);

        // Assert : l'événement peut être repris
        assertTrue(
                secondClaim.stream()
                        .anyMatch(event ->
                                event.id().equals(claimedEvent.id())
                        )
        );

        // Vérifier le nouveau propriétaire
        UUID actualWorker = jdbcTemplate.queryForObject(
                """
                SELECT claimed_by
                FROM payment_outbox
                WHERE id = ?
                """,
                UUID.class,
                claimedEvent.id()
        );

        assertEquals(workerB, actualWorker);
    }
    @Test
    void shouldNotMarkEventAsPublishedWhenWorkerLostClaim() {

        var payment = paymentService.createPayment(
                new PaymentCreateDTO(
                        "TEST_SYSTEM",
                        UUID.randomUUID().toString(),
                        "BR-001",
                        new BigDecimal("150.00"),
                        "USD"
                )
        );

        UUID workerA = UUID.randomUUID();
        UUID workerB = UUID.randomUUID();

        var firstClaim = repository.claimPendingEvents(100, workerA);

        UUID eventId = firstClaim.stream()
                .filter(event ->
                        event.paymentReference().equals(payment.reference())
                )
                .findFirst()
                .orElseThrow()
                .id();

        // Simuler l'expiration de la réservation du worker A
        jdbcTemplate.update("""
            UPDATE payment_outbox
            SET claimed_until = CURRENT_TIMESTAMP - INTERVAL '1 minute'
            WHERE id = ?
            """, eventId);

        // Le worker B récupère l'événement
        var secondClaim = repository.claimPendingEvents(100, workerB);

        assertTrue(secondClaim.stream()
                .anyMatch(event -> event.id().equals(eventId)));

        // L'ancien worker tente de confirmer la publication
        repository.markAsPublished(eventId, workerA);

        // L'événement doit rester non publié
        Boolean published = jdbcTemplate.queryForObject("""
            SELECT published_at IS NOT NULL
            FROM payment_outbox
            WHERE id = ?
            """, Boolean.class, eventId);

        assertFalse(published);
    }
}