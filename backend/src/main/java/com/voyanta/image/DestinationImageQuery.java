package com.voyanta.image;

import com.voyanta.survey.enums.InterestType;

import java.util.Set;

/**
 * Destinasiya şəkli axtarışı üçün bütün məlumat bir yerdə.
 *
 * Əvvəl ImageService yalnız (destination, country) alırdı. Bu, ən mühüm məlumatı —
 * USE SİTİNİ NÖVÜNÜ (təbiət / dəniz / tarix) — itirməyə səbəb olurdu, çünki olmayan
 * şəkli seçimi üçün "təbiət" və "tarix" arasında fərq qoymaq mümkün deyildi.
 *
 * <p>Bu record bütün işləyicilər (destination kartları, tövsiyələr, wishlist, plan)
 * üçün ümumi qarərdır: heç bir modul konkret təchizatçını tanımır, yalnız bu konteksti
 * doldurur.
 *
 * @param destination partnering adı (məs. "Misir", "Greenland") — null/boş ola bilər
 * @param country     ölkə (məs. "Azərbaycan") — null/boş ola bilər
 * @param tag         kart etiketi (məs. "Təbiət", "Tarix və mədəniyyət") — null/boş ola bilər
 * @param interests   strukturlaşdırılmış maraq teqləri (NATURE / SEA / ...) — null/boş ola bilər
 */
public record DestinationImageQuery(
        String destination,
        String country,
        String tag,
        Set<InterestType> interests
) {

    public DestinationImageQuery {
        interests = interests == null ? Set.of() : Set.copyOf(interests);
    }

    /** Sadə çağırış: ad + ölkə (plan örtüyü kimi tag/maraq olmayan yollar üçün). */
    public static DestinationImageQuery of(String destination, String country) {
        return new DestinationImageQuery(destination, country, null, Set.of());
    }

    /** Destinyasiya kartları üçün tam kontekst. */
    public static DestinationImageQuery of(String destination, String country,
                                           String tag, Set<InterestType> interests) {
        return new DestinationImageQuery(destination, country, tag, interests);
    }
}