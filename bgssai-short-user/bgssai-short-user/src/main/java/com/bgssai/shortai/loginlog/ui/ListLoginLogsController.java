package com.bgssai.shortai.loginlog.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bgssai.shortai.common.PageResponse;
import com.bgssai.shortai.common.aop.NeedAop;
import com.bgssai.shortai.loginlog.service.ListLoginLogsService;

/**
 * List the current user's own login logs (paged).
 * GET /bgssai/user/login-logs?page_num=1&page_size=20
 * Returns a PageResponse whose result is a list of login-log items.
 * @NeedAop makes JwtTokenAspect validate the JWT and populate UserContext before the method
 * runs; the current-user scoping lives in the service. This is the reference for a protected,
 * single-endpoint Controller (Standards §1 / §3): it only binds params and delegates.
 */
@RestController
public class ListLoginLogsController {

    @Autowired
    private ListLoginLogsService listLoginLogsService;

    @NeedAop
    @GetMapping("/bgssai/user/login-logs")
    public PageResponse list(@RequestParam(name = "page_num", defaultValue = "1") int pageNum,
                             @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return listLoginLogsService.list(pageNum, pageSize);
    }
}
