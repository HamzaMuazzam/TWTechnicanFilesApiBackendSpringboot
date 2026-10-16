package com.trackingworld.tw.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackingworld.tw.common.ApiPaths;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.ResponseWrapper;
import com.trackingworld.tw.common.config.SwaggerConfig;
import com.trackingworld.tw.common.exceptions.BadRequestException;
import com.trackingworld.tw.common.exceptions.FieldValidationException;
import com.trackingworld.tw.common.web.ClientIp;
import com.trackingworld.tw.alerts.domain.Configuration;
import com.trackingworld.tw.alerts.repo.ConfigurationRepository;
import com.trackingworld.tw.common.exceptions.NotFoundException;
import com.trackingworld.tw.ingest.dto.ConfigurationDto;
import com.trackingworld.tw.ingest.dto.IngestAlertRequest;
import com.trackingworld.tw.ingest.dto.IngestConfigurationRequest;
import com.trackingworld.tw.ingest.dto.IngestResultDto;
import com.trackingworld.tw.security.IngestSourcePrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping(ApiPaths.INGEST)
@RequiredArgsConstructor
@Tag(name = "Ingest", description = "S3 -> TW Notification: configurations and alerts")
@SecurityRequirement(name = SwaggerConfig.API_KEY)
public class IngestController {

    public static final int MAX_BATCH = 500;

    private final IngestService ingestService;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final ConfigurationRepository configurationRepository;

    @Operation(summary = "Push configurations: one object or an array (max 500). Upserts by S3's ID.",
            description = "Fields: ID, PhoneNumber, UID, AlertType (omit or '*' = all types), DBDT, CrtBY.")
    @PostMapping("/configurations")
    public ResponseEntity<ResponseWrapper<List<ConfigurationDto>>> configurations(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "One configuration object, or an array of them (max 500).",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = IngestConfigurationRequest.class)),
                            examples = @ExampleObject(name = "Two configurations for tracker 111", value = """
                                    [
                                      { "ID": "C-1", "PhoneNumber": "03001234567", "UID": "111", "AlertType": "*", "DBDT": "2026-10-14T09:00:00+05:00", "CrtBY": "s3" },
                                      { "ID": "C-2", "PhoneNumber": "03211234567", "UID": "111", "AlertType": "OVERSPEED", "CrtBY": "s3" }
                                    ]""")))
            @RequestBody JsonNode body) {
        List<IngestConfigurationRequest> items = parse(body, IngestConfigurationRequest.class);
        List<ConfigurationDto> saved = new ArrayList<>(items.size());
        for (IngestConfigurationRequest item : items) {
            saved.add(ConfigurationDto.of(ingestService.saveConfiguration(item)));
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ResponseWrapper.success(saved));
    }

    @Operation(summary = "List configurations, optionally for one uid")
    @GetMapping("/configurations")
    public ResponseWrapper<List<ConfigurationDto>> listConfigurations(@RequestParam(required = false) String uid) {
        List<Configuration> rows = uid == null || uid.isBlank()
                ? configurationRepository.findAll(Sort.by("id")) : configurationRepository.findByUidOrderByIdAsc(uid.trim());
        return ResponseWrapper.success(rows.stream().map(ConfigurationDto::of).toList());
    }

    @Operation(summary = "Delete a configuration by S3's ID (or our numeric id)")
    @DeleteMapping("/configurations/{id}")
    public ResponseWrapper<Boolean> deleteConfiguration(@PathVariable String id) {
        Configuration cfg = configurationRepository.findBySourceId(id)
                .or(() -> parseLong(id).flatMap(configurationRepository::findById))
                .orElseThrow(() -> new NotFoundException(ErrorCodes.NOT_FOUND));
        configurationRepository.delete(cfg);
        log.info("[INGEST] configuration {} deleted", id);
        return ResponseWrapper.success(true);
    }

    private static Optional<Long> parseLong(String s) {
        try {
            return Optional.of(Long.parseLong(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Operation(summary = "Push alerts: one object or an array (max 500). Recipients are resolved from the configurations.",
            description = "Fields: ID, UID, AlertType, Message, Lat, Lon, DBDT. Result NO_CONFIGURATION means nobody is configured for that uid/type.")
    @PostMapping("/alerts")
    public ResponseEntity<ResponseWrapper<List<IngestResultDto>>> ingest(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "One alert object, or an array of them (max 500).",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = IngestAlertRequest.class)),
                            examples = @ExampleObject(name = "Two alerts for tracker 111", value = """
                                    [
                                      { "ID": "A-1", "UID": "111", "AlertType": "OVERSPEED", "Message": "Vehicle ABC-123 exceeded 120 km/h", "Lat": 24.8607, "Lon": 67.0011, "DBDT": "2026-10-14T09:05:00+05:00" },
                                      { "ID": "A-2", "UID": "111", "AlertType": "SOS", "Message": "SOS pressed", "Lat": 24.86, "Lon": 67.0 }
                                    ]""")))
            @RequestBody JsonNode body,
                                                                         @AuthenticationPrincipal IngestSourcePrincipal source,
                                                                         HttpServletRequest request) {
        List<IngestAlertRequest> items = parse(body, IngestAlertRequest.class);
        Map<String, String> missingIds = new LinkedHashMap<>();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id() == null || items.get(i).id().isBlank()) {
                missingIds.put("[" + i + "].id", "must not be blank (S3 alert ID, used to de-duplicate)");
            }
        }
        if (!missingIds.isEmpty()) {
            throw new FieldValidationException(missingIds);
        }
        log.info("[INGEST] source={} ({}) from={} items={}", source.name(), source.id(), ClientIp.of(request), items.size());
        List<IngestResultDto> results = ingestService.ingest(source.id(), items);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ResponseWrapper.success(results));
    }

    private <T> List<T> parse(JsonNode body, Class<T> type) {
        if (body == null || body.isNull() || body.isMissingNode()) {
            throw new BadRequestException(ErrorCodes.EMPTY_BATCH);
        }
        List<T> items;
        if (body.isArray()) {
            if (body.isEmpty()) {
                throw new BadRequestException(ErrorCodes.EMPTY_BATCH);
            }
            if (body.size() > MAX_BATCH) {
                throw new BadRequestException(ErrorCodes.BATCH_TOO_LARGE);
            }
            items = new ArrayList<>(body.size());
            for (JsonNode node : body) {
                items.add(objectMapper.convertValue(node, type));
            }
        } else if (body.isObject()) {
            items = List.of(objectMapper.convertValue(body, type));
        } else {
            throw new BadRequestException(ErrorCodes.MALFORMED_REQUEST);
        }
        Map<String, String> errors = new LinkedHashMap<>();
        for (int i = 0; i < items.size(); i++) {
            for (ConstraintViolation<T> v : validator.validate(items.get(i))) {
                errors.put("[" + i + "]." + v.getPropertyPath(), v.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        return items;
    }
}
