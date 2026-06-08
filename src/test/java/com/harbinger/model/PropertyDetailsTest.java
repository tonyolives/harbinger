package com.harbinger.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PropertyDetailsTest {

    private static PropertyDetails details(
            int yearBuilt,
            int beds,
            int baths,
            int lotSizeSqFt,
            long assessedValue,
            long estimatedMarketValue,
            long mortgageBalance,
            long equity) {
        return new PropertyDetails(
                PropertyType.SINGLE_FAMILY, yearBuilt, beds, baths, lotSizeSqFt,
                assessedValue, estimatedMarketValue, mortgageBalance, equity);
    }

    @Test
    void buildsWithValidFields() {
        PropertyDetails details = details(1998, 3, 2, 6000, 400_000L, 500_000L, 200_000L, 300_000L);

        assertThat(details.propertyType()).isEqualTo(PropertyType.SINGLE_FAMILY);
        assertThat(details.yearBuilt()).isEqualTo(1998);
        assertThat(details.beds()).isEqualTo(3);
        assertThat(details.baths()).isEqualTo(2);
        assertThat(details.lotSizeSqFt()).isEqualTo(6000);
        assertThat(details.assessedValue()).isEqualTo(400_000L);
        assertThat(details.estimatedMarketValue()).isEqualTo(500_000L);
        assertThat(details.mortgageBalance()).isEqualTo(200_000L);
        assertThat(details.equity()).isEqualTo(300_000L);
    }

    @Test
    void allowsZeroEquityAndZeroMortgage() {
        // A fully mortgaged home has zero equity; an unmortgaged one has zero balance.
        assertThat(details(2000, 3, 2, 6000, 400_000L, 500_000L, 500_000L, 0L).equity()).isZero();
        assertThat(details(2000, 3, 2, 6000, 400_000L, 500_000L, 0L, 500_000L).mortgageBalance())
                .isZero();
    }

    @Test
    void rejectsNullPropertyType() {
        assertThatThrownBy(() -> new PropertyDetails(
                        null, 2000, 3, 2, 6000, 400_000L, 500_000L, 200_000L, 300_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("propertyType");
    }

    @Test
    void rejectsNonPositiveYearBuilt() {
        assertThatThrownBy(() -> details(0, 3, 2, 6000, 400_000L, 500_000L, 200_000L, 300_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yearBuilt");
    }

    @Test
    void rejectsNonPositiveBeds() {
        assertThatThrownBy(() -> details(2000, 0, 2, 6000, 400_000L, 500_000L, 200_000L, 300_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("beds");
    }

    @Test
    void rejectsNonPositiveBaths() {
        assertThatThrownBy(() -> details(2000, 3, 0, 6000, 400_000L, 500_000L, 200_000L, 300_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baths");
    }

    @Test
    void rejectsNonPositiveLotSize() {
        assertThatThrownBy(() -> details(2000, 3, 2, 0, 400_000L, 500_000L, 200_000L, 300_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lotSizeSqFt");
    }

    @Test
    void rejectsNegativeAssessedValue() {
        assertThatThrownBy(() -> details(2000, 3, 2, 6000, -1L, 500_000L, 200_000L, 300_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("assessedValue");
    }

    @Test
    void rejectsNegativeMarketValue() {
        assertThatThrownBy(() -> details(2000, 3, 2, 6000, 400_000L, -1L, 200_000L, 300_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estimatedMarketValue");
    }

    @Test
    void rejectsNegativeMortgageBalance() {
        assertThatThrownBy(() -> details(2000, 3, 2, 6000, 400_000L, 500_000L, -1L, 300_000L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mortgageBalance");
    }

    @Test
    void rejectsNegativeEquity() {
        assertThatThrownBy(() -> details(2000, 3, 2, 6000, 400_000L, 500_000L, 200_000L, -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("equity");
    }
}
