package com.trackingworld.tw.admin.dto;

import java.util.List;

public record SqlResultDto(List<String> columns, List<List<Object>> rows, boolean truncated, long elapsedMs) {
}
