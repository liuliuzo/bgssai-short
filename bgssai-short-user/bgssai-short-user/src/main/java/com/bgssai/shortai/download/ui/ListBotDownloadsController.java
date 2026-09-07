package com.bgssai.shortai.download.ui;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.ApiResponse;
import com.bgssai.shortai.download.service.ListBotDownloadsService;

/**
 * GET /api/download/bot
 *
 * <p>公开：下载页要在登录之前就能打开，故不加 {@code @NeedAop}。</p>
 */
@RestController
public class ListBotDownloadsController {

    private final ListBotDownloadsService listBotDownloadsService;

    public ListBotDownloadsController(ListBotDownloadsService listBotDownloadsService) {
        this.listBotDownloadsService = listBotDownloadsService;
    }

    @GetMapping("/api/download/bot")
    public ApiResponse list() {
        ApiResponse response = new ApiResponse();
        response.setCode("0");
        response.setMessage("OK");
        response.setSuccess(true);
        response.setResult(listBotDownloadsService.list());
        return response;
    }
}
