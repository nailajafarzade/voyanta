package com.voyanta.user.service;


import com.voyanta.auth.dao.entity.User;
import com.voyanta.auth.dao.repository.UserRepository;
import com.voyanta.common.exception.ResourceNotFoundException;

import com.voyanta.user.dto.request.UpdateProfileRequest;
import com.voyanta.user.dto.response.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service responsible for reading and updating a user's profile. It loads the user by id
 * through the auth module's repository, saves the new name and phone, and returns the
 * data as a UserProfileResponse.
 *
 * Diqqət: auth.repository.UserRepository birbaşa istifadə olunur — bu icazəlidir,
 * repository-lər modullar arasında paylaşıla bilər. Amma auth.service.AuthService
 * heç vaxt bura import olunmayacaq — autentifikasiya məntiqi yalnız auth-un işidir,
 * bu modul yalnız profil oxuyub-yazır.
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        return toResponse(findUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        userRepository.save(user);
        return toResponse(user);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi tapılmadı"));
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getCreatedAt()
        );
    }
}