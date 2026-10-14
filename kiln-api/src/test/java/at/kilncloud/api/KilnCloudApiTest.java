package at.kilncloud.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests the API root type. */
final class KilnCloudApiTest {
    /** Verifies that the API root stays a public interface so consumers never depend on an implementation. */
    @Test
    @DisplayName("KilnCloudApi ist ein öffentliches Interface ohne Implementierung")
    void kilnCloudApi_isPublicInterface() {
        Class<?> api = KilnCloudApi.class;
        assertTrue(api.isInterface());
        assertTrue(Modifier.isPublic(api.getModifiers()));
    }
}
