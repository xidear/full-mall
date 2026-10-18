package com.fullmall.legal.api;

import com.fullmall.legal.app.LegalService;
import com.fullmall.legal.domain.LegalDocument;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/legal")
public class LegalController {

    private final LegalService legalService;

    public LegalController(LegalService legalService) {
        this.legalService = legalService;
    }

    @GetMapping("/documents")
    public LegalDocument document(@RequestParam String type,
                                  @RequestParam(defaultValue = "zh-CN") String locale,
                                  @RequestParam(defaultValue = "CN") String market) {
        return legalService.findActive(type, locale, market);
    }

    @PostMapping("/acceptances")
    @ResponseStatus(HttpStatus.CREATED)
    public LegalAcceptance accept(@Valid @RequestBody LegalAcceptanceRequest request,
                                  HttpServletRequest httpRequest) {
        return legalService.accept(request, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
    }
}