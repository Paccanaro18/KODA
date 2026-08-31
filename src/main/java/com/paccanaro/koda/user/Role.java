package com.paccanaro.koda.user;

/**
 * Papeis do sistema. A autorizacao e sempre verificada no servidor; o frontend
 * apenas reflete o que o backend ja autorizou (SEC-03).
 */
public enum Role {
    STUDENT,
    MODERATOR,
    ADMIN
}
