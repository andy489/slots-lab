package com.slotslab.reels.api;

public record ApiResponse(String result, String error) {
    public static ApiResponse ok(String result) { return new ApiResponse(result, null); }
    public static ApiResponse err(String message) { return new ApiResponse(null, message); }
}
