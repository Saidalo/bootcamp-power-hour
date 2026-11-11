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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
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
    void mapsZonePricesPer15MinutesAndConvertsToKwh() {
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

        List<PricePoint> result = provider.fetchDayAhead(BiddingZone.EE, LocalDate.of(2026, 11, 4));

        assertThat(result).hasSize(5);
        assertThat(result.get(0).startsAt().toInstant()).isEqualTo(Instant.ofEpochSecond(DAY_START));
        assertThat(result.get(0).priceEurKwh()).isEqualByComparingTo("0.010");
        assertThat(result.get(1).startsAt().toInstant()).isEqualTo(Instant.ofEpochSecond(DAY_START + 900));
        assertThat(result.get(4).priceEurKwh()).isEqualByComparingTo("0.050");
        server.verify();
    }

    @Test
    void throwsProviderExceptionWhenEleringIsDown() {
        server.expect(requestTo(startsWith(URL))).andRespond(withServerError());

        assertThrows(PriceProviderException.class,
                () -> provider.fetchDayAhead(BiddingZone.EE, LocalDate.of(2026, 11, 4)));
    }

    @Test
    void requestsTheWholeBalticDayInUtc() {
        server.expect(requestTo(startsWith(URL)))
                .andExpect(queryParam("start", "2026-11-03T22:00:00Z"))
                .andExpect(queryParam("end", "2026-11-04T21:59:59Z"))
                .andRespond(withSuccess("{\"success\": true, \"data\": {\"ee\": []}}", MediaType.APPLICATION_JSON));

        provider.fetchDayAhead(BiddingZone.EE, LocalDate.of(2026, 11, 4));

        server.verify();
    }

    @Test
    void retriesWhenFirstRequestFails() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://dashboard.elering.ee");
        MockRestServiceServer retryServer = MockRestServiceServer.bindTo(builder).build();
        EleringPriceProvider retryingProvider = new EleringPriceProvider(builder.build(), 2, 0);

        retryServer.expect(requestTo(startsWith(URL))).andRespond(withServerError());
        retryServer.expect(requestTo(startsWith(URL))).andRespond(withSuccess(
                "{\"success\": true, \"data\": {\"ee\": [{\"timestamp\": %d, \"price\": 42.0}]}}".formatted(DAY_START),
                MediaType.APPLICATION_JSON));

        List<PricePoint> result = retryingProvider.fetchDayAhead(BiddingZone.EE, LocalDate.of(2026, 11, 4));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).priceEurKwh()).isEqualByComparingTo("0.042");
        retryServer.verify();
    }

    @Test
    void throwsWhenEleringReportsUnsuccessfulResponse() {
        server.expect(requestTo(startsWith(URL)))
                .andRespond(withSuccess("{\"success\": false, \"data\": null}", MediaType.APPLICATION_JSON));

        assertThrows(PriceProviderException.class,
                () -> provider.fetchDayAhead(BiddingZone.EE, LocalDate.of(2026, 11, 4)));
    }

    @Test
    void skipsSlotsWithoutPrice() {
        String json = "{\"success\": true, \"data\": {\"ee\": [{\"timestamp\": %d, \"price\": null}, {\"timestamp\": %d, \"price\": 5.0}]}}"
                .formatted(DAY_START, DAY_START + 900);
        server.expect(requestTo(startsWith(URL))).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<PricePoint> result = provider.fetchDayAhead(BiddingZone.EE, LocalDate.of(2026, 11, 4));

        assertThat(result).extracting(p -> p.startsAt().toInstant())
                .containsExactly(Instant.ofEpochSecond(DAY_START + 900));
    }

    @Test
    void returnsNoPricesWhenZoneIsMissingFromResponse() {
        server.expect(requestTo(startsWith(URL)))
                .andRespond(withSuccess("{\"success\": true, \"data\": {\"ee\": []}}", MediaType.APPLICATION_JSON));

        assertThat(provider.fetchDayAhead(BiddingZone.LT, LocalDate.of(2026, 11, 4))).isEmpty();
    }
}