package com.voyanta.survey.enums;

// Diqqət: SIGHTSEEING (Görməli yerlər) sorğudan çıxarılıb, NIGHTLIFE (Gecə həyatı) əlavə olunub.
// Mövcud DB sətirlərində "SIGHTSEEING" dəyəri qalmış olsa (JSONB-də), onu oxumaq artıq
// Jackson deserialize xətası verəcək — pre-production data-dırsa problem deyil,
// real istifadəçi datası varsa əvvəlcə təmizləmək lazımdır.
public enum InterestType {
    NATURE, SEA, HISTORY_CULTURE, NIGHTLIFE
}