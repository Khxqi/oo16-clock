package dev.clock.font.rs;

import android.graphics.Canvas;
import android.graphics.Typeface;
import android.util.Log;
import android.widget.TextView;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Modern LSPosed 102, scoped only to com.android.systemui.
 * Target StatClock was confirmed in the user's exact OxygenOS 16 SystemUI.apk.
 * This does NOT hook android.widget.TextView globally.
 */
public final class ClockModule extends XposedModule {
    private static final String TAG = "RSClock";
    private static final String TARGET_PACKAGE = "com.android.systemui";
    private static final String STAT_CLOCK = "com.oplus.systemui.statusbar.widget.StatClock";
    private static final String KEYGUARD_CLOCK = "com.oplus.systemui.statusbar.widget.OplusKeyguardStatusBarClock";
    private static final Map<TextView, Boolean> LOGGED =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile boolean hooksInstalled = false;
    private static volatile boolean fontAttempted = false;
    private static volatile Typeface font;

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        if (!TARGET_PACKAGE.equals(param.getPackageName())) return;
        synchronized (ClockModule.class) {
            if (hooksInstalled) return;
            try {
                // Exact class present in classes4.dex of OxygenOS 16 SystemUI 16.99.12.
                Class<?> cls = Class.forName(STAT_CLOCK, false, param.getDefaultClassLoader());
                if (!TextView.class.isAssignableFrom(cls)) {
                    Log.e(TAG, "StatClock unexpectedly does not extend TextView");
                    return;
                }
                Method draw = cls.getDeclaredMethod("onDraw", Canvas.class);
                hook(draw).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            try {
                                Object receiver = chain.getThisObject();
                                if (receiver instanceof TextView) applyIfTarget((TextView) receiver);
                            } catch (Throwable problem) {
                                Log.w(TAG, "Typeface skipped safely during StatClock.onDraw", problem);
                            }
                            return chain.proceed();
                        });
                // onDraw also runs after rotation/theme changes: resets are corrected then.
                hooksInstalled = true;
                Log.i(TAG, "StatClock.onDraw hook installed on OxygenOS 16 (Modern API 102)");
            } catch (Throwable problem) {
                Log.e(TAG, "StatClock hook failed; no system UI resources modified", problem);
            }
        }
    }

    private static void applyIfTarget(TextView view) {
        // Never change the dedicated keyguard clock, QS header or any generic TextView.
        // The hooked method is declared only by Oplus StatClock; these checks also
        // protect against unexpected subclasses or future firmware changes.
        String actualClass = view.getClass().getName();
        if (KEYGUARD_CLOCK.equals(actualClass) || !STAT_CLOCK.equals(actualClass)) return;
        Typeface requested = getFont(view.getContext());
        if (requested == null || view.getTypeface() == requested) return;
        view.setTypeface(requested);
        if (!LOGGED.containsKey(view)) {
            LOGGED.put(view, Boolean.TRUE);
            Log.i(TAG, "Applied custom Roboto-RSf font to " + actualClass);
        }
    }

    private static Typeface getFont(android.content.Context context) {
        if (fontAttempted) return font;
        synchronized (ClockModule.class) {
            if (!fontAttempted) {
                font = EmbeddedFont.load(context);
                fontAttempted = true;
            }
        }
        return font;
    }
}
