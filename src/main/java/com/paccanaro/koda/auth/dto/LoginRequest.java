package com.paccanaro.koda.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "E-mail e obrigatorio")
        @Size(max = 254)
        String email,

        @NotBlank(message = "Senha e obrigatoria")
        @Size(max = 128)
        String password
) {
}
