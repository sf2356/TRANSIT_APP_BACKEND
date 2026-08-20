package com.transit.platform.common;

/** Enveloppe standard pour toute réponse API réussie (donnée unique). */
public record ApiResponse<T>(boolean success, T data) {
    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(true, data);
    }
}
