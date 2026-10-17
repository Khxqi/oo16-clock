package dev.clock.font.rs;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Loads the user's bundled Roboto-RSf.ttf through a supported public API.
 * SystemUI cannot directly read another app's private files, so the font is
 * copied into SystemUI's own device-protected private cache before loading it.
 * Nothing under /system, /system_ext or /sdcard is modified.
 */
final class EmbeddedFont {
    private static final String TAG = "RSClock";
    private static final String RESOURCE = "fonts/Roboto-RSf.ttf";
    private static final String CACHE_FILE = "rs-clock-roboto-rsf-v1.ttf";
    private static final int MAX_FONT_BYTES = 8 * 1024 * 1024;

    private EmbeddedFont() { }

    static Typeface load(Context context) {
        if (context == null) {
            Log.e(TAG, "No SystemUI context available to load clock font");
            return null;
        }
        Context deviceProtected = context.createDeviceProtectedStorageContext();
        File cache = deviceProtected.getCacheDir();
        File target = new File(cache, CACHE_FILE);
        File temporary = null;
        try (InputStream in = EmbeddedFont.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                Log.e(TAG, "Embedded font missing from module APK: " + RESOURCE);
                return null;
            }
            temporary = File.createTempFile("rs-clock-", ".ttf", cache);
            int total = 0;
            try (FileOutputStream out = new FileOutputStream(temporary)) {
                byte[] buffer = new byte[16384];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    total += count;
                    if (total > MAX_FONT_BYTES) throw new IOException("Font exceeds size limit");
                    out.write(buffer, 0, count);
                }
                if (total == 0) throw new IOException("Font resource is empty");
                out.getFD().sync();
            }

            try {
                Files.move(temporary.toPath(), target.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(temporary.toPath(), target.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            return new Typeface.Builder(target).build();
        } catch (Throwable error) {
            Log.e(TAG, "Failed to load custom font from SystemUI private cache", error);
            return null;
        } finally {
            if (temporary != null && temporary.exists() && !temporary.delete()) {
                Log.w(TAG, "Could not delete temporary font file");
            }
        }
    }
}
