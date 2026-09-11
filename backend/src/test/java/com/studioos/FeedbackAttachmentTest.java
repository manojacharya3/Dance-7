package com.studioos;

import com.studioos.service.FeedbackAttachmentService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure unit tests for screenshot validation — no Spring context, no database. */
class FeedbackAttachmentTest {

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final byte[] WEBP = {0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50};
    private static final byte[] SCRIPT = {'<', 's', 'c', 'r', 'i', 'p', 't', '>', 0, 0, 0, 0};
    private static final byte[] ELF = {0x7F, 'E', 'L', 'F', 0, 0, 0, 0, 0, 0, 0, 0};

    @Test
    void magicBytesAccepted() throws Exception {
        var m = method();
        assertTrue((Boolean) m.invoke(null, PNG, "image/png"));
        assertTrue((Boolean) m.invoke(null, JPEG, "image/jpeg"));
        assertTrue((Boolean) m.invoke(null, WEBP, "image/webp"));
    }

    @Test
    void executablesAndScriptsRejected() throws Exception {
        var m = method();
        assertFalse((Boolean) m.invoke(null, SCRIPT, "image/png"));
        assertFalse((Boolean) m.invoke(null, ELF, "image/png"));
        assertFalse((Boolean) m.invoke(null, PNG, "image/jpeg"));
        assertFalse((Boolean) m.invoke(null, PNG, "application/octet-stream"));
        assertFalse((Boolean) m.invoke(null, new byte[4], "image/png"));
        assertFalse((Boolean) m.invoke(null, null, "image/png"));
    }

    @Test
    void fileNamesSanitized() throws Exception {
        var m = FeedbackAttachmentService.class.getDeclaredMethod("sanitizeFileName", String.class);
        m.setAccessible(true);
        assertEquals("photo.png", m.invoke(null, "../../etc/photo.png"));
        assertEquals("screenshot", m.invoke(null, (Object) null));
        assertEquals("my_shot_1.jpg", m.invoke(null, "my shot 1.jpg"));
        assertTrue(((String) m.invoke(null, "x".repeat(300) + ".png")).length() <= 200);
    }

    @Test
    void extensionsParsed() throws Exception {
        var m = FeedbackAttachmentService.class.getDeclaredMethod("extensionOf", String.class);
        m.setAccessible(true);
        assertEquals("png", m.invoke(null, "shot.PNG"));
        assertEquals("jpeg", m.invoke(null, "a.b.jpeg"));
        assertEquals("", m.invoke(null, "noextension"));
    }

    @Test
    void limitsDocumented() {
        assertEquals(10L * 1024 * 1024, FeedbackAttachmentService.MAX_FILE_SIZE);
        assertEquals(5, FeedbackAttachmentService.MAX_PER_FEEDBACK);
    }

    private java.lang.reflect.Method method() throws Exception {
        var m = FeedbackAttachmentService.class.getDeclaredMethod("matchesMagicBytes", byte[].class, String.class);
        m.setAccessible(true);
        return m;
    }
}
