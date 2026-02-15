package com.cisnebranco.dto.response;

import java.util.List;

public record ClientResponse(
        Long id,
        String name,
        String phone,
        String email,
        String address,
        List<PetResponse> pets
) {}
