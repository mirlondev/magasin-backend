package org.odema.posnew.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        String error
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Opération réussie", data, null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static ApiResponse<Void> error(String error) {
        return new ApiResponse<>(false, null, null, error);
    }
}