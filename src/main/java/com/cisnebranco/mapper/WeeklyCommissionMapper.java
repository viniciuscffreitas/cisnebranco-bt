package com.cisnebranco.mapper;

import com.cisnebranco.dto.response.WeeklyCommissionResponse;
import com.cisnebranco.entity.WeeklyCommission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WeeklyCommissionMapper {

    @Mapping(source = "groomer.id", target = "groomerId")
    @Mapping(source = "groomer.name", target = "groomerName")
    WeeklyCommissionResponse toResponse(WeeklyCommission commission);
}
