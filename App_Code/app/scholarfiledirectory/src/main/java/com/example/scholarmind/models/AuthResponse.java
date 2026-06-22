package com.example.scholarmind.models;

public class AuthResponse {
    public String access_token;
    public String refresh_token;
    public String token_type;
    public User user;

    public static class User {
        public String uid;
        public String full_name;
        public String email;
        public boolean is_verified;
    }
}