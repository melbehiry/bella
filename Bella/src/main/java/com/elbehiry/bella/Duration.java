package com.elbehiry.bella;

import androidx.annotation.IntDef;

@IntDef({Duration.LENGTH_SHORT, Duration.LENGTH_LONG})
public @interface Duration {
    int LENGTH_SHORT = 1000;
    int LENGTH_LONG = 2000;
}
