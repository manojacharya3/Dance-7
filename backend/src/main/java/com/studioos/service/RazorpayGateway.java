package com.studioos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Razorpay REST integration without third-party SDKs: order creation over
 * HTTPS (Basic auth) and HMAC-SHA256 signature verification with JCA.
 * All secrets come from environment; none are logged or returned to clients.
 */
@Service
public class RazorpayGateway {
    private static final Logger log = LoggerFactory.getLogger(RazorpayGateway.class);

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final String currency;
    private final boolean enabled;
    private final RestClient rest;
    private final ObjectMapper mapper = new ObjectMapper();

    public RazorpayGateway(
        @Value("${dance7.razorpay.key-id:}") String keyId,
        @Value("${dance7.razorpay.key-secret:}") String keySecret,
        @Value("${dance7.razorpay.webhook-secret:}") String webhookSecret,
        @Value("${dance7.razorpay.currency:INR}") String currency,
        @Value("${dance7.razorpay.enabled:false}") boolean enabled) {
        this.keyId = keyId == null ? "" : keyId.trim();
        this.keySecret = keySecret == null ? "" : keySecret.trim();
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
        this.currency = currency == null || currency.isBlank() ? "INR" : currency.trim().toUpperCase();
        this.enabled = enabled && !this.keyId.isEmpty() && !this.keySecret.isEmpty();
        ClientHttpRequestFactory factory = ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
            .withConnectTimeout(Duration.ofSeconds(5)).withReadTimeout(Duration.ofSeconds(15)));
        String credentials = Base64.getEncoder()
            .encodeToString((this.keyId + ":" + this.keySecret).getBytes(StandardCharsets.UTF_8));
        this.rest = RestClient.builder().baseUrl("https://api.razorpay.com/v1").requestFactory(factory)
            .defaultHeader("Authorization", "Basic " + credentials).build();
    }

    public boolean isEnabled() { return enabled; }
    public String getKeyId() { return keyId; }
    public String getCurrency() { return currency; }

    public void requireEnabled() {
        if (!enabled) throw new IllegalStateException("Online payments are not configured. Set RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET and DANCE7_RAZORPAY_ENABLED=true.");
    }

    /** Amount in paise (minor units). */
    public static long toPaise(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    /** Creates a Razorpay order; returns the order id. */
    public String createOrder(long amountPaise, String receipt) {
        requireEnabled();
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("amount", amountPaise);
            body.put("currency", currency);
            body.put("receipt", receipt);
            String raw = rest.post().uri("/orders").contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(String.class);
            String orderId = mapper.readTree(raw).path("id").asText("");
            if (orderId.isEmpty()) throw new IllegalStateException("Razorpay did not return an order id.");
            return orderId;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Razorpay order creation failed: {}", e.getClass().getSimpleName());
            throw new IllegalStateException("Could not reach Razorpay. Please try again shortly.");
        }
    }

    /** Verifies checkout signature: HMAC_SHA256(order_id|payment_id, key_secret). */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        if (orderId == null || paymentId == null || signature == null) return false;
        return constantTimeEquals(hmacHex(keySecret, orderId + "|" + paymentId), signature.trim());
    }

    /** Verifies webhook signature: HMAC_SHA256(raw_body, webhook_secret). */
    public boolean verifyWebhookSignature(String rawBody, String signature) {
        if (webhookSecret.isEmpty() || rawBody == null || signature == null) return false;
        return constantTimeEquals(hmacHex(webhookSecret, rawBody), signature.trim());
    }

    public static String hmacHex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(out.length * 2);
            for (byte b : out) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Signature computation failed.");
        }
    }

    public static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
            a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
