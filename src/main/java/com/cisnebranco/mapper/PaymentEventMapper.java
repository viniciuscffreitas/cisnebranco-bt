package com.cisnebranco.mapper;

import com.cisnebranco.dto.response.PaymentEventResponse;
import com.cisnebranco.entity.PaymentEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentEventMapper {

    @Mapping(source = "createdBy.username", target = "createdByUsername")
    PaymentEventResponse toResponse(PaymentEvent event);
}
