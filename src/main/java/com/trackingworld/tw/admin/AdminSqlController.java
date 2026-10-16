package com.trackingworld.tw.admin;

import com.trackingworld.tw.admin.dto.SqlRequest;
import com.trackingworld.tw.admin.dto.SqlResultDto;
import com.trackingworld.tw.admin.sql.SqlConsoleService;
import com.trackingworld.tw.common.ApiPaths;
import com.trackingworld.tw.common.ResponseWrapper;
import com.trackingworld.tw.common.web.ClientIp;
import com.trackingworld.tw.security.AdminPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admin - SQL console")
public class AdminSqlController {

    private final SqlConsoleService sqlConsoleService;

    @Operation(summary = "Read-only SQL (SELECT/WITH, single statement, 10s timeout, max 1000 rows). ADMIN only.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(ApiPaths.ADMIN + "/sql")
    public ResponseWrapper<SqlResultDto> run(@Valid @RequestBody SqlRequest request,
                                             @AuthenticationPrincipal AdminPrincipal principal,
                                             HttpServletRequest http) {
        return ResponseWrapper.success(sqlConsoleService.run(request.sql(), principal.username(), ClientIp.of(http)));
    }

    @ExceptionHandler(SqlConsoleService.SqlConsoleException.class)
    public ResponseEntity<ResponseWrapper<String>> handleSql(SqlConsoleService.SqlConsoleException ex) {
        return ResponseEntity.status(ex.getHttpStatus()).body(ResponseWrapper.error(ex.getErrorCode(), ex.getReason()));
    }
}
