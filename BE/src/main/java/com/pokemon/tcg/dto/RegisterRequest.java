package com.pokemon.tcg.dto;

public class RegisterRequest {
    private String screenName;
    private String email;
    private String password;
    private String confirmPassword;

    public String getScreenName() { return screenName; }
    public void setScreenName(String screenName) { this.screenName = screenName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
