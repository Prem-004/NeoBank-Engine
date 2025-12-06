package com.neobankengine.service;

import com.neobankengine.dto.UserRegisterRequest;
import com.neobankengine.entity.User;
import com.neobankengine.exception.BadRequestException;
import com.neobankengine.exception.ConflictException;
import com.neobankengine.exception.ResourceNotFoundException;
import com.neobankengine.repository.UserRepository;
import com.neobankengine.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Register a new user.
     * Throws ConflictException if email already exists.
     */
    public String register(UserRegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered!");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        user.setStatus(true);

        userRepository.save(user);

        return "User Registered Successfully!";
    }

    /**
     * Simple login that returns a message.
     * Throws ResourceNotFoundException or BadRequestException on failure.
     */
    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Invalid password!");
        }

        return "Login successful!";
    }

    /**
     * Login and return JWT token.
     * Throws ResourceNotFoundException or BadRequestException on failure.
     */
    public String loginAndGetToken(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Invalid password!");
        }

        // include email as subject in token; JwtUtil handles signing
        return jwtUtil.generateToken(user.getEmail());
    }

}
