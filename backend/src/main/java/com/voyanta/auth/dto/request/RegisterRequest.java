package com.voyanta.auth.dto.request;

/*
 * MÜVƏQQƏTİ İSTİFADƏ OLUNMUR — qeydiyyat yalnız Google ilədir, email/şifrə tamamilə çıxıb.
 * AuthController/AuthService-də register() geri açılarsa, bu da geri açılmalıdır.
 *
 * import jakarta.validation.constraints.Email;
 * import jakarta.validation.constraints.NotBlank;
 * import jakarta.validation.constraints.Size;
 *
 * public record RegisterRequest(
 *         @NotBlank(message = "Ad Soyad boş ola bilməz")
 *         String fullName,
 *
 *         @NotBlank
 *         @Email(message = "Düzgün e-poçt daxil edin")
 *         String email,
 *
 *         String phone,
 *
 *         @NotBlank
 *         @Size(min = 8, message = "Şifrə minimum 8 simvol olmalıdır")
 *         String password
 * ) {}
 */