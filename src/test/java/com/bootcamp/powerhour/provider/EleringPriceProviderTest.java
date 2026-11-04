package com.bootcamp.powerhour.provider;

import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.exception.PriceProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class EleringPriceProviderTest {
    private static final String URL = "https://dashboard.elering.ee/api/nps/price";
    private static final long DAY_START = Instant.parse("2026-11-03T22:00:00Z").getEpochSecond();

    private MockRestServiceServer server;
    private EleringPriceProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://dashboard.elering.ee");
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new EleringPriceProvider(builder.build(), 1, 0);
    }

    @Test
    void mapsZonePricesAveragesQuarterHoursAndConvertsToKwh() {
        String json = """
                {"success": true, "data": {
                  "ee": [
                    {"timestamp": %d, "price": 1.0},
                    {"timestamp": %d, "price": 10.0},
                    {"timestamp": %d, "price": 20.0},
                    {"timestamp": %d, "price": 30.0},
                    {"timestamp": %d, "price": 40.0},
                    {"timestamp": %d, "price": 50.0}
                  ],
                  "lv": [{"timestamp": %d, "price": 999.0}]
                }}
                """.formatted(
                DAY_START - 3600,
                DAY_START, DAY_START + 900, DAY_START + 1800, DAY_START + 2700,
                DAY_START + 3600,
                DAY_START);

        server.expect(requestTo(startsWith(URL)))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<HourlyPricePoint> result = provider.fetchDayAhead(BiddingZone.EE, LocalDate.of(2026, 11, 4));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).startsAt().toInstant()).isEqualTo(Instant.ofEpochSecond(DAY_START));
        assertThat(result.get(0).priceEurKwh()).isEqualByComparingTo("0.025");
        assertThat(result.get(1).priceEurKwh()).isEqualByComparingTo("0.050");
        server.verify();
    }

    @Test
    void throwsProviderExceptionWhenEleringIsDown() {
        server.expect(requestTo(startsWith(URL))).andRespond(withServerError());

        assertThrows(PriceProviderException.class,
                () -> provider.fetchDayAhead(BiddingZone.EE, LocalDate.of(2026, 11, 4)));
    }
}