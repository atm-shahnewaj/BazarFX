package com.bazarfx.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class User {
    private String id;
    private String username;
    private String email;
    private String phone;
    private String location;
    private String passwordHash;
    private String passwordSalt;
    private String profilePhotoPath;
    private double sellerRatingAvg;
    private int sellerRatingCount;
    private long createdAt;
    private List<String> wishlistProductIds = new ArrayList<>();

    public User() {}

    public User(String id, String username, String email, String phone, String location,
                String passwordHash, String passwordSalt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.location = location;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.createdAt = Instant.now().toEpochMilli();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getPasswordSalt() { return passwordSalt; }
    public void setPasswordSalt(String passwordSalt) { this.passwordSalt = passwordSalt; }
    public String getProfilePhotoPath() { return profilePhotoPath; }
    public void setProfilePhotoPath(String profilePhotoPath) { this.profilePhotoPath = profilePhotoPath; }
    public double getSellerRatingAvg() { return sellerRatingAvg; }
    public void setSellerRatingAvg(double sellerRatingAvg) { this.sellerRatingAvg = sellerRatingAvg; }
    public int getSellerRatingCount() { return sellerRatingCount; }
    public void setSellerRatingCount(int sellerRatingCount) { this.sellerRatingCount = sellerRatingCount; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public List<String> getWishlistProductIds() { return wishlistProductIds; }
    public void setWishlistProductIds(List<String> wishlistProductIds) { this.wishlistProductIds = wishlistProductIds; }
}
