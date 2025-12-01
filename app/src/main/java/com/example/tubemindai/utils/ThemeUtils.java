package com.example.tubemindai.utils;

import android.content.Context;
import android.content.res.Configuration;

/**
 * Utility class for theme management
 */
public class ThemeUtils {

    /**
     * Check if device is in dark mode
     */
    public static boolean isDarkMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Get appropriate background drawable based on theme
     */
    public static int getBackgroundDrawable(Context context) {
        if (isDarkMode(context)) {
            return context.getResources().getIdentifier(
                    "gradient_background_dark", "drawable", context.getPackageName());
        } else {
            return context.getResources().getIdentifier(
                    "gradient_background", "drawable", context.getPackageName());
        }
    }

    /**
     * Get appropriate edit text background based on theme
     */
    public static int getEditTextBackground(Context context) {
        if (isDarkMode(context)) {
            return context.getResources().getIdentifier(
                    "rounded_edittext_dark", "drawable", context.getPackageName());
        } else {
            return context.getResources().getIdentifier(
                    "rounded_edittext", "drawable", context.getPackageName());
        }
    }
}

