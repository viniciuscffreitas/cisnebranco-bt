package com.cisnebranco.dto.request;

import jakarta.validation.constraints.NotNull;

public record AddServiceItemRequest(
    @NotNull Long serviceTypeId
) {}
