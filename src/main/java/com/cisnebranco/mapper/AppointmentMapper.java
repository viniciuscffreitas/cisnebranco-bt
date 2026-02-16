package com.cisnebranco.mapper;

import com.cisnebranco.dto.response.AppointmentResponse;
import com.cisnebranco.entity.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ClientMapper.class, PetMapper.class, GroomerMapper.class, ServiceTypeMapper.class})
public interface AppointmentMapper {

    @Mapping(source = "technicalOs.id", target = "technicalOsId")
    AppointmentResponse toResponse(Appointment appointment);
}
