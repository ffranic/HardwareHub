package hr.tvz.ntp.smartordersystem.controller;

import hr.tvz.ntp.smartordersystem.model.Role;
import hr.tvz.ntp.smartordersystem.model.User;
import hr.tvz.ntp.smartordersystem.service.LogHelperService;
import hr.tvz.ntp.smartordersystem.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final LogHelperService logHelperService;

    public UserController(UserService userService,  LogHelperService logHelperService) {
        this.userService = userService;
        this.logHelperService = logHelperService;
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        if (users.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> findById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<User> saveUser(@Valid @RequestBody User user,
                                         Authentication authentication,
                                         HttpServletRequest request) {
        User savedUser = userService.saveUser(user);

        logHelperService.log(
                authentication,
                request,
                "CREATE_USER",
                "Created user with id " + savedUser.getId() + " and username " + savedUser.getUsername()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id,
                                           @Valid @RequestBody User user,
                                           Authentication authentication,
                                           HttpServletRequest request) {
        User updatedUser = userService.updateUser(id, user);

        logHelperService.log(
                authentication,
                request,
                "UPDATE_USER_PROFILE",
                "Updated user profile with id " + id
        );

        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<User> updateUserRole(@PathVariable Long id,
                                               @RequestBody Map<String, String> requestBody,
                                               Authentication authentication,
                                               HttpServletRequest request) {
        Role role = Role.valueOf(requestBody.get("role"));
        User updatedUser = userService.updateRole(id, role);

        logHelperService.log(
                authentication,
                request,
                "UPDATE_USER_ROLE",
                "Changed role for user id " + id + " to " + role
        );

        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<?> updatePassword(@PathVariable Long id,
                                            @RequestBody Map<String, String> requestBody,
                                            Authentication authentication,
                                            HttpServletRequest request) {
        String newPassword = requestBody.get("newPassword");

        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "New password must not be empty"));
        }

        userService.updatePassword(id, newPassword);

        logHelperService.log(
                authentication,
                request,
                "UPDATE_PASSWORD",
                "Updated password for user id " + id
        );

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserById(@PathVariable Long id,
                               Authentication authentication,
                               HttpServletRequest request) {

        userService.deleteUser(id);

        logHelperService.log(
                authentication,
                request,
                "DELETE_USER",
                "Deleted user with id " + id
        );
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> handleNoSuchElementException(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

}