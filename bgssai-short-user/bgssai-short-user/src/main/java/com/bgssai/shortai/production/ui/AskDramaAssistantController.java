package com.bgssai.shortai.production.ui;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.common.Responses;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.production.service.DramaAssistantService;

@RestController
public class AskDramaAssistantController {

    private final DramaAssistantService service;

    public AskDramaAssistantController(DramaAssistantService service) {
        this.service = service;
    }

    @NeedAop
    @PostMapping("/bgssai/user/drama-assistant/ask")
    public ApiResponse handle(@RequestBody Map<String, Object> body) {
        Map<String, Object> payload = body == null ? Map.of() : body;
        return Responses.ok(service.ask(
                text(payload.get("role")),
                text(payload.get("message")),
                text(payload.get("context")),
                text(payload.get("model_code"))));
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
