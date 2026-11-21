package hu.forex.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/soap")
    public String soap() {
        return "soap";
    }

    @GetMapping("/forex/account")
    public String forexAccount() {
        return page("Forex-account", "A kapcsolódó OANDA-számlainformációk itt jelennek majd meg.");
    }

    @GetMapping("/forex/price")
    public String forexPrice() {
        return page("Forex-AktÁr", "Instrumentum kiválasztása és aktuális árfolyam lekérdezése.");
    }

    @GetMapping("/forex/history")
    public String forexHistory() {
        return page("Forex-HistÁr", "Historikus árfolyamok lekérdezése instrumentum és granularitás alapján.");
    }

    @GetMapping("/forex/open")
    public String forexOpen() {
        return page("Forex-Nyit", "Piaci pozíció nyitása pozitív (Long) vagy negatív (Short) mennyiséggel.");
    }

    @GetMapping("/forex/positions")
    public String forexPositions() {
        return page("Forex-Poz", "A nyitott pozíciók táblázata itt jelenik majd meg.");
    }

    @GetMapping("/forex/close")
    public String forexClose() {
        return page("Forex-Zár", "Pozíció lezárása tradeId alapján.");
    }

    private String page(String title, String description) {
        return page(title, description, new org.springframework.ui.ConcurrentModel());
    }

    private String page(String title, String description, Model model) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("pageDescription", description);
        return "placeholder";
    }
}
