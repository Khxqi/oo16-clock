package dev.clock.font.rs;

import android.graphics.Typeface;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Loads the user's Roboto-RSf.ttf from the module APK as a Java resource.
 * Do not read it from /sdcard or another app's files: SystemUI is isolated.
 */
final class EmbeddedFont {
    private static final String TAG = "RSClock";
    private static final String RESOURCE = "fonts/Roboto-RSf.ttf";

    private EmbeddedFont() { }

    static Typeface load() {
        try (InputStream in = EmbeddedFont.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                Log.e(TAG, "Font resource missing from module APK: " + RESOURCE);
                return null;
            }
            ByteArrayOutputStream data = new ByteArrayOutputStream(2500000);
            byte[] buffer = new byte[16384];
            int length;
            while ((length = in.read(buffer)) != -1) {
                data.write(buffer, 0, length);
                if (data.size() > 8 * 1024 * 1024) {
                    throw new IOException("Font is unexpectedly large");
                }
            }
            Typeface face = new Typeface.Builder(ByteBuffer.wrap(data.toByteArray())).build();
            if (face == null) Log.e(TAG, "Typeface.Builder could not load custom font");
            return face;
        } catch (Throwable problem) {
            Log.e(TAG, "Could not read embedded Roboto-RSf font", problem);
            return null;
        }
    }
}
