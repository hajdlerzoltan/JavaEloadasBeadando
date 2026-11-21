package hu.forex.app.controller;

import com.fasterxml.jackson.databind.JsonNode;
import hu.forex.app.service.OandaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/oanda")
public class OandaController {
    private final OandaService service;

    public OandaController(OandaService service) { this.service = service; }

    @GetMapping("/account")
    public JsonNode account() { return service.getAccountSummary(); }

    @GetMapping("/price")
    public JsonNode price(@RequestParam(defaultValue = "EUR_USD") String instrument) {
        return service.getCurrentPrice(instrument);
    }

    @GetMapping("/history")
    public JsonNode history(@RequestParam(defaultValue = "EUR_USD") String instrument,
                            @RequestParam(defaultValue = "D") String granularity) {
        return service.getHistoricalPrices(instrument, granularity);
    }

    @PostMapping("/open")
    public JsonNode open(@RequestParam String instrument, @RequestParam BigDecimal units) {
        return service.openPosition(instrument, units);
    }

    @GetMapping("/positions")
    public JsonNode positions() { return service.getOpenPositions(); }

    @PostMapping("/close")
    public JsonNode close(@RequestParam String tradeId) { return service.closePosition(tradeId); }

    @ExceptionHandler(OandaService.OandaApiException.class)
    public ResponseEntity<Map<String, Object>> oandaError(OandaService.OandaApiException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("error", "OANDA API hiba", "details", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> configuration(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
    }
}
