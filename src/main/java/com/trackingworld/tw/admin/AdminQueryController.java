package com.trackingworld.tw.admin;

import com.trackingworld.tw.admin.dto.FilterRequest;
import com.trackingworld.tw.admin.dto.PageResult;
import com.trackingworld.tw.admin.query.QueryService;
import com.trackingworld.tw.common.ApiPaths;
import com.trackingworld.tw.common.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admin - Query")
public class AdminQueryController {

    private final QueryService queryService;

    @Operation(summary = "Generic filtered/sorted/paged query over alerts, notifications, deliveries, users, devices, "
            + "discards, sources, alert-types, admins")
    @PostMapping(ApiPaths.ADMIN + "/query/{entity}")
    public ResponseWrapper<PageResult<Object>> query(@PathVariable String entity,
                                                     @RequestBody(required = false) @Valid FilterRequest request) {
        return ResponseWrapper.success(queryService.query(entity, request == null ? empty() : request));
    }

    @Operation(summary = "Same filter as /query, streamed as CSV (max 50 000 rows)")
    @PostMapping(ApiPaths.ADMIN + "/export/{entity}")
    public ResponseEntity<StreamingResponseBody> export(@PathVariable String entity,
                                                        @RequestBody(required = false) @Valid FilterRequest request) {
        FilterRequest effective = request == null ? empty() : request;
        queryService.validate(entity, effective);
        String filename = entity + "-" + LocalDate.now() + ".csv";
        StreamingResponseBody body = out -> queryService.exportCsv(entity, effective, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }

    private static FilterRequest empty() {
        return new FilterRequest(List.of(), "AND", List.of(), 0, FilterRequest.DEFAULT_SIZE, null);
    }
}
