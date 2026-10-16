package com.voyanta.user.dto.request;

import jakarta.validation.constraints.NotBlank;

// Email bura salınmayıb — email dəyişmək təsdiq (verification) tələb edən
// ayrıca axındır, sadə profil yeniləməsi ilə qarışdırılmamalıdır.
/**
 * Request DTO for updating the signed-in user's profile. It only carries the editable
 * fields, full name and phone, and requires the name to be non-empty.
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Ad Soyad boş ola bilməz")
        String fullName,

        String phone
) {}