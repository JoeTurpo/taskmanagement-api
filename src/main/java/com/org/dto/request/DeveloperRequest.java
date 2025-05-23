package com.org.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeveloperRequest(
        @NotBlank(message = "Nombre es obligatorio")
        @Size(max = 100, message = "No debe exceder los 100 caracteres")
        String name
        ) {}
