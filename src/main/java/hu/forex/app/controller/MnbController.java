
package hu.forex.app.controller;

import hu.forex.app.model.ExchangeRate;
import hu.forex.app.service.MnbSoapService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mnb")
public class MnbController {

    private final MnbSoapService mnbSoapService;

    public MnbController(MnbSoapService mnbSoapService) {
        this.mnbSoapService = mnbSoapService;
    }

    @GetMapping("/rates")
    public ResponseEntity<?> getRates(

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam
            String currency) {

        try {
            List<ExchangeRate> rates =
                    mnbSoapService.getExchangeRates(
                            startDate,
                            endDate,
                            currency
                    );

            return ResponseEntity.ok(rates);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );

        } catch (Exception e) {

            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "error",
                            e.getMessage() == null
                                    ? "Ismeretlen hiba történt."
                                    : e.getMessage()
                    )
            );
        }
    }
}
