package com.paccanaro.koda.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Politica de senha: comprimento minimo generoso em vez de regras de composicao.
 * Segue a orientacao do NIST SP 800-63B — exigir simbolo/maiuscula produz senhas
 * previsiveis ("Senha123!") sem ganho real de entropia.
 */
public record RegisterRequest(

        @NotBlank(message = "E-mail e obrigatorio")
        @Email(message = "E-mail invalido")
        @Size(max = 254, message = "E-mail longo demais")
        String email,

        @NotBlank(message = "Senha e obrigatoria")
        @Size(min = 12, max = 128, message = "A senha precisa ter entre 12 e 128 caracteres")
        String password,

        @Size(max = 60, message = "Nome longo demais")
        String displayName
) {
}
