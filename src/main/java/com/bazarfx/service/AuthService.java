package com.bazarfx.service;

import com.bazarfx.model.User;
import com.bazarfx.storage.FileStorageManager;
import com.bazarfx.util.IdGenerator;
import com.bazarfx.util.InputValidator;
import com.bazarfx.util.PasswordHasher;

import java.util.Optional;

public class AuthService {

    private final FileStorageManager storage;
    private User currentUser;

    public AuthService(FileStorageManager storage) {
        this.storage = storage;
    }

    public static class AuthException extends RuntimeException {
        public AuthException(String message) { super(message); }
    }

    public User register(String username, String email, String phone, String location, String password) {
        if (!InputValidator.isValidUsername(username)) throw new AuthException("Username must be 3-30 characters.");
        if (!InputValidator.isValidEmail(email)) throw new AuthException("Enter a valid email address.");
        if (!InputValidator.isValidPhone(phone)) throw new AuthException("Enter a valid phone number.");
        if (!InputValidator.isNonEmpty(location)) throw new AuthException("Location is required.");
        if (!InputValidator.isValidPassword(password)) throw new AuthException("Password must be at least 6 characters.");

        boolean usernameTaken = storage.usersMap().values().stream()
                .anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
        if (usernameTaken) throw new AuthException("That username is already taken.");

        boolean emailTaken = storage.usersMap().values().stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
        if (emailTaken) throw new AuthException("An account with that email already exists.");

        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(password, salt);
        User user = new User(IdGenerator.newId(), username, email, phone, location, hash, salt);
        storage.saveUser(user);
        return user;
    }

    public User login(String usernameOrEmail, String password) {
        Optional<User> match = storage.usersMap().values().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(usernameOrEmail)
                        || u.getEmail().equalsIgnoreCase(usernameOrEmail))
                .findFirst();

        User user = match.orElseThrow(() -> new AuthException("No account found with that username or email."));
        if (!PasswordHasher.matches(password, user.getPasswordSalt(), user.getPasswordHash())) {
            throw new AuthException("Incorrect password.");
        }
        currentUser = user;
        return user;
    }

    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() { return currentUser; }

    public void updateProfile(User user, String phone, String location, String profilePhotoPath) {
        user.setPhone(phone);
        user.setLocation(location);
        if (profilePhotoPath != null) user.setProfilePhotoPath(profilePhotoPath);
        storage.saveUser(user);
    }
}
