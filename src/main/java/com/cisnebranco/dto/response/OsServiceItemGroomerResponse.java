package com.cisnebranco.dto.response;

import java.math.BigDecimal;

public record OsServiceItemGroomerResponse(
        Long id,
        String serviceTypeName,
        BigDecimal lockedPrice
) {}
