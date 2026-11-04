package com.bootcamp.powerhour.provider;

import com.bootcamp.powerhour.entity.BiddingZone;
import com.bootcamp.powerhour.exception.PriceProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class EleringPriceProvider implements PriceProvider {
    private static final BigDecimal KWH_PER_MWH = BigDecimal.valueOf(1000);
    private static final Logger log = LoggerFactory.getLogger(EleringPriceProvider.class);
    private static final ZoneId BALTIC_ZONE = ZoneId.of("Europe/Tallinn");
    private final RestClient restClient;
    private final int maxAttempts;
    private final long retryDelayMs;

    public EleringPriceProvider (@Qualifier("eleringRestClient") RestClient restClient, @Value("${elering.max-attempts}") int maxAttempts, @Value("${elering.retry-delay-ms}") long retryDelayMs){
        this.restClient = restClient;
        this.maxAttempts = maxAttempts;
        this.retryDelayMs = retryDelayMs;
    }

    @Override
    public List<PricePoint> fetchDayAhead(BiddingZone zone, LocalDate date){
        Instant start = date.atStartOfDay(BALTIC_ZONE).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(BALTIC_ZONE).toInstant();

        EleringResponse response = callWithRetry(start, end.minusSeconds(1));
        if (response == null || !response.success() || response.data() == null){
            throw new PriceProviderException("Elering returned an unsuccessful response for " + zone + " " + date);
        }

        return response.data()
                .getOrDefault(zone.name().toLowerCase(Locale.ROOT), List.of())
                .stream()
                .filter(p -> p.price() != null)
                .filter(p -> {
                    Instant time = Instant.ofEpochSecond(p.timestamp());
                    return !time.isBefore(start) && time.isBefore(end);
                })
                .sorted(Comparator.comparingLong(EleringResponse.EleringPrice::timestamp))
                .map(p -> new PricePoint (
                        OffsetDateTime.ofInstant(Instant.ofEpochSecond(p.timestamp()), ZoneOffset.UTC),
                        p.price().divide(KWH_PER_MWH, 6, RoundingMode.HALF_UP)))
                .toList();
    }

    private EleringResponse callWithRetry (Instant start, Instant end){
        RestClientException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++){
            try {
                return restClient.get().uri(uri -> uri.path("/api/nps/price").queryParam("start", start.toString()).queryParam("end", end.toString()).build()).retrieve().body(EleringResponse.class);
            } catch (RestClientException e) {
                lastError = e;
                log.warn("Elering request failed (attempt {}/{}): {}", attempt, maxAttempts, e.getMessage());
                if (attempt < maxAttempts) {
                    sleep(retryDelayMs * attempt);
                }
            }
        }
        throw new PriceProviderException("Elering API unavailable after " + maxAttempts + " attempts", lastError);
    }

    private static void sleep (long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PriceProviderException("Interrupted while waiting to retry", e);
        }
    }
}
