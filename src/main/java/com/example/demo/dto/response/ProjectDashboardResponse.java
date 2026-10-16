package com.example.demo.dto.response;

import java.util.List;

public record ProjectDashboardResponse(long pending, long executed, long lost, long cancelled, List<ProjectDashboardYearResponse> yearly) { }
