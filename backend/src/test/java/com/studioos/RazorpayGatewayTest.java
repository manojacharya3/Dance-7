package com.studioos;

import com.studioos.service.RazorpayGateway;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure unit tests for Razorpay cryptography helpers — no network, no secrets. */
class RazorpayGatewayTest {

    @Test
    void hmacVectorsAndVerification() {
        // Razorpay documents: signature = HMAC_SHA256(order_id|payment_id, key_secret).
        String secret = "test_secret_key";
        String payload = "order_9A33XWu170gUtm|pay_29QQoUBi66xm2fH";
        String signature = RazorpayGateway.hmacHex(secret, payload);
        assertEquals(64, signature.length());
        assertTrue(RazorpayGateway.constantTimeEquals(signature, signature.toLowerCase()));
        assertFalse(RazorpayGateway.constantTimeEquals(signature, signature + "00"));
        assertFalse(RazorpayGateway.constantTimeEquals("ab", "abc"));
    }

    @Test
    void paiseConversion() {
        assertEquals(250000L, RazorpayGateway.toPaise(new BigDecimal("2500")));
        assertEquals(675000L, RazorpayGateway.toPaise(new BigDecimal("6750.00")));
        assertEquals(1300500L, RazorpayGateway.toPaise(new BigDecimal("13005")));
    }

    @Test
    void gatewayDisabledWithoutKeys() {
        RazorpayGateway gateway = new RazorpayGateway("", "", "", "INR", true);
        assertFalse(gateway.isEnabled());
    }

    @Test
    void nullSignaturesRejected() {
        assertFalse(RazorpayGateway.constantTimeEquals("", "abc"));
    }
}
