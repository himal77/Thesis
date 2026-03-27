package com.iot.processor.service;

import org.springframework.stereotype.Component;

/**
 * Classifies a reading based on its Z-score.
 * Used by the alert-engine to decide alert severity.
 */
@Component
public class ReadingClassifier {

    public String classify(double zScore) {
        double abs = Math.abs(zScore);
        if      (abs >= 4.0) return "CRITICAL";
        else if (abs >= 3.0) return "HIGH";
        else if (abs >= 2.5) return "MEDIUM";
        else if (abs >= 1.5) return "LOW";
        else                 return "NORMAL";
    }
}
