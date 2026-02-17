package com.cisnebranco.service;

import com.cisnebranco.dto.request.CreateUserRequest;
import com.cisnebranco.dto.response.UserResponse;
import com.cisnebranco.entity.AppUser;
import com.cisnebranco.entity.Groomer;
import com.cisnebranco.entity.enums.UserRole;
import com.cisnebranco.exception.BusinessException;
import com.cisnebranco.exception.ResourceNotFoundException;
import com.cisnebranco.repository.AppUserRepository;
import com.cisnebranco.repository.GroomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;
    private final GroomerRepository groomerRepository;
    private final PasswordEncoder passwordEncoder;
    private final SseEmitterService sseEmitterService;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username already exists: " + request.username());
        }

        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());

        if (request.role() == UserRole.GROOMER) {
            if (request.groomerId() == null) {
                throw new BusinessException("Groomer ID is required for GROOMER role");
            }
            Groomer groomer = groomerRepository.findById(request.groomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Groomer", request.groomerId()));
            user.setGroomer(groomer);
        }

        AppUser saved = userRepository.save(user);
        sseEmitterService.broadcastAfterCommit("user-changed", "created", saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deactivate(Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setActive(false);
        userRepository.save(user);
        sseEmitterService.broadcastAfterCommit("user-changed", "deactivated", id);
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getGroomer() != null ? user.getGroomer().getId() : null,
                user.getGroomer() != null ? user.getGroomer().getName() : null,
                user.isActive()
        );
    }
}
