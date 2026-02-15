package com.cisnebranco.mapper;

import com.cisnebranco.dto.response.PricingMatrixResponse;
import com.cisnebranco.entity.PricingMatrix;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PricingMatrixMapper {

    @Mapping(source = "serviceType.id", target = "serviceTypeId")
    @Mapping(source = "serviceType.name", target = "serviceTypeName")
    PricingMatrixResponse toResponse(PricingMatrix pricingMatrix);
}
