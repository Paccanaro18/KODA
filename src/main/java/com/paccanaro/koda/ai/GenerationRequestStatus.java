package com.paccanaro.koda.ai;

/** Ciclo de vida de um pedido na fila de pre-geracao. */
public enum GenerationRequestStatus {
    PENDING,
    PROCESSING,
    DONE,
    FAILED
}
