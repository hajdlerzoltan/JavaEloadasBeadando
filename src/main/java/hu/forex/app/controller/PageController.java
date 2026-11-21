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
    public String forexAccount(Model model) {
        model.addAttribute("activePage", "account");
        model.addAttribute("pageTitle", "Forex-account");
        return "forex";
    }

    @GetMapping("/forex/price")
    public String forexPrice(Model model) {
        model.addAttribute("activePage", "price");
        model.addAttribute("pageTitle", "Forex-AktÁr");
        return "forex";
    }

    @GetMapping("/forex/history")
    public String forexHistory(Model model) {
        model.addAttribute("activePage", "history");
        model.addAttribute("pageTitle", "Forex-HistÁr");
        return "forex";
    }

    @GetMapping("/forex/open")
    public String forexOpen(Model model) {
        model.addAttribute("activePage", "open");
        model.addAttribute("pageTitle", "Forex-Nyit");
        return "forex";
    }

    @GetMapping("/forex/positions")
    public String forexPositions(Model model) {
        model.addAttribute("activePage", "positions");
        model.addAttribute("pageTitle", "Forex-Poz");
        return "forex";
    }

    @GetMapping("/forex/close")
    public String forexClose(Model model) {
        model.addAttribute("activePage", "close");
        model.addAttribute("pageTitle", "Forex-Zár");
        return "forex";
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
