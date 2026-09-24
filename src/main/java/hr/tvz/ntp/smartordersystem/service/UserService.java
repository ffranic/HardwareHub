package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.model.Role;
import hr.tvz.ntp.smartordersystem.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<User> getAllUsers();
    Optional<User> getUserById(long id);
    User saveUser(User user);
    User updateUser(Long id, User updatedUser);
    void deleteUser(Long id);
    void updatePassword(Long id, String newPassword);
    User updateRole(Long id, Role role);
}
