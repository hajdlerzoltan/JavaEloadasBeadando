package hu.forex.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

@Service
public class OandaService {
    private final RestClient client;
    private final ObjectMapper mapper;
    private final String accountId;
    private final String token;

    public OandaService(
            @Value("${oanda.api.url:${oanda.base-url:}}") String baseUrl,
            @Value("${oanda.account.id:${oanda.account-id:}}") String accountId,
            @Value("${oanda.api.token:${oanda.api-token:}}") String token,
            ObjectMapper mapper) {
        this.accountId = accountId.trim();
        this.token = token.trim();
        this.mapper = mapper;
        this.client = RestClient.builder().baseUrl(baseUrl.replaceAll("/+$", "")).build();
    }

    private void checkConfiguration() {
        if (token.isBlank() || accountId.isBlank()) {
            throw new IllegalStateException("Hiányzik az OANDA token vagy account ID az application.properties fájlból.");
        }
    }

    private JsonNode send(String method, String path, Object body) {
        checkConfiguration();
        try {
            String response = switch (method) {
                case "GET" -> client.get().uri(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().body(String.class);
                case "POST" -> client.post().uri(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(String.class);
                case "PUT" -> client.put().uri(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(String.class);
                default -> throw new IllegalArgumentException("Nem támogatott HTTP-metódus");
            };
            return mapper.readTree(response == null ? "{}" : response);
        } catch (RestClientResponseException e) {
            // Nem naplózzuk a tokent; a szolgáltató válasza segít a hiba felismerésében.
            throw new OandaApiException(e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (OandaApiException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("OANDA-kapcsolati hiba: " + e.getMessage(), e);
        }
    }

    private String accountPath() { return "/v3/accounts/" + accountId; }

    private String instrument(String input) {
        String normalized = input == null ? "" : input.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}_[A-Z]{3}")) {
            throw new IllegalArgumentException("Érvénytelen instrumentum, példa: EUR_USD");
        }
        return normalized;
    }

    public JsonNode getAccountSummary() {
        return send("GET", accountPath() + "/summary", null);
    }

    public JsonNode getCurrentPrice(String instrument) {
        String uri = UriComponentsBuilder.fromPath(accountPath() + "/pricing")
                .queryParam("instruments", instrument(instrument)).build().toUriString();
        return send("GET", uri, null);
    }

    public JsonNode getHistoricalPrices(String instrument, String granularity) {
        String g = granularity == null ? "" : granularity.trim().toUpperCase(Locale.ROOT);
        if (!g.matches("M1|H1|D|W|M")) {
            throw new IllegalArgumentException("Granularity: M1, H1, D, W vagy M lehet.");
        }
        // A tanári példával egyezően 10 középáras gyertya; ez a végpont nem account-specifikus.
        String uri = UriComponentsBuilder.fromPath("/v3/instruments/" + instrument(instrument) + "/candles")
                .queryParam("count", 10).queryParam("granularity", g)
                .queryParam("price", "M").build().toUriString();
        return send("GET", uri, null);
    }

    public JsonNode openPosition(String instrument, BigDecimal units) {
        if (units == null || units.signum() == 0 || units.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("A mennyiség nem nulla egész szám legyen; pozitív = Long, negatív = Short.");
        }
        return send("POST", accountPath() + "/orders", Map.of("order", Map.of(
                "type", "MARKET", "instrument", instrument(instrument),
                "units", units.toPlainString(), "timeInForce", "FOK", "positionFill", "DEFAULT")));
    }

    public JsonNode getOpenPositions() {
        // A feladat tradeId alapján zár: ezért nyitott trade-eket adunk vissza.
        return send("GET", accountPath() + "/openTrades", null);
    }

    public JsonNode closePosition(String tradeId) {
        String id = tradeId == null ? "" : tradeId.trim();
        if (!id.matches("[0-9]+")) {
            throw new IllegalArgumentException("A tradeId csak számjegyeket tartalmazhat.");
        }
        return send("PUT", accountPath() + "/trades/" + id + "/close", Map.of("units", "ALL"));
    }

    public static class OandaApiException extends RuntimeException {
        private final int status;
        public OandaApiException(int status, String message) {
            super(message);
            this.status = status;
        }
        public int getStatus() { return status; }
    }
}
