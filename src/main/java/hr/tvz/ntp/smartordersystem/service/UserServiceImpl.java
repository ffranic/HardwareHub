package hr.tvz.ntp.smartordersystem.service;

import hr.tvz.ntp.smartordersystem.model.Role;
import hr.tvz.ntp.smartordersystem.model.User;
import hr.tvz.ntp.smartordersystem.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPreHashService passwordPreHashService;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           PasswordPreHashService passwordPreHashService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPreHashService = passwordPreHashService;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> getUserById(long id) {
        return userRepository.findById(id);
    }

    @Override
    public User saveUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        user.setRole(Role.CUSTOMER);
        String preHashedPassword = passwordPreHashService.preHashPassword(
                user.getUsername(),
                user.getPassword()
        );

        user.setPassword(passwordEncoder.encode(preHashedPassword));
        return userRepository.save(user);
    }

    @Override
    public User updateUser(Long id, User updatedUser) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        if (userRepository.existsByUsernameAndIdNot(updatedUser.getUsername(), id)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmailAndIdNot(updatedUser.getEmail(), id)) {
            throw new IllegalArgumentException("Email already exists");
        }

        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setEmail(updatedUser.getEmail());

        return userRepository.save(existingUser);
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NoSuchElementException("User not found");
        }

        userRepository.deleteById(id);
    }

    @Override
    public void updatePassword(Long id, String newPassword) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        String preHashedPassword = passwordPreHashService.preHashPassword(
                existingUser.getUsername(),
                newPassword
        );

        existingUser.setPassword(passwordEncoder.encode(preHashedPassword));
        userRepository.save(existingUser);
    }

    @Override
    public User updateRole(Long id, Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        user.setRole(role);
        return userRepository.save(user);
    }
}
