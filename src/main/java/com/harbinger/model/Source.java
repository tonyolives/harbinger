package com.harbinger.model;

/**
 * Where a raw public-record signal came from. These are the messy upstream
 * systems a real pipeline would scrape; here they obviouslu label synthetic data.
 */
public enum Source {
    COUNTY_RECORDER,
    COURT,
    ASSESSOR,
    TAX_OFFICE
}
