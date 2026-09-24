package hr.tvz.ntp.smartordersystem.service;

public interface PasswordPreHashService {

    String preHashPassword(String username, String rawPassword);

    String preHashPasswordWithPepper(String username, String rawPassword, String pepper);

    String findMatchingPepper(String username, String rawPassword, String storedBcryptHash);
}
