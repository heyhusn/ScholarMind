package com.example.scholarmind.models;

public class SignupRequest {
    public String full_name;
    public String email;
    public String password;
    public boolean terms_accepted;

    public SignupRequest(String full_name, String email, String password, boolean terms_accepted) {
        this.full_name = full_name;
        this.email = email;
        this.password = password;
        this.terms_accepted = terms_accepted;
    }
}
