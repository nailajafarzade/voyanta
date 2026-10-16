package com.voyanta.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyanta.ai.dto.request.AiRequest;
import com.voyanta.survey.dto.shared.Budget;
import com.voyanta.survey.dto.shared.FamilyDetails;
import com.voyanta.survey.dto.shared.TravelDates;
import com.voyanta.survey.enums.BudgetTier;
import com.voyanta.survey.enums.InterestType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class PromptBuilder {

    // Modelin "yalnız səyahət planlaşdırması" hüdudundan çıxmaması üçün — istifadəçidən
    // heç bir sərbəst mətn AI-yə getmir, yalnız survey-dən qurulan bu structured summary.
    static final String SYSTEM_PROMPT = """
        Sən Voyanta səyahət planlaşdırma tətbiqinin AI köməkçisisən.

        VƏZİFƏ
        İstifadəçinin cavablarına (maraqlar, yoldaş, büdcə, tarix, otel tipi,
        yemək üstünlüyü, səyahətin məqsədi) əsasən bir destinasiya seç və gündəlik
        səyahət planı hazırla.

        Cavabı yalnız verilən JSON schema formatında qaytar.
        Səyahət planlaşdırmasına aid olmayan heç bir sorğuya cavab vermə.


        DESTİNASİYA SEÇİMİ
        - Destinasiyanı yalnız istifadəçinin cavablarına görə seç: maraqlara,
          yoldaşa, büdcəyə, tarixə (fəsil və hava şəraiti daxil) və səyahətin
          məqsədinə uyğun gəlməlidir.
        - Dünyanın bütün regionlarını nəzərdən keçir. Cavablar dəyişdikdə
          destinasiya seçimini də yenidən qiymətləndir.
        - Eyni destinasiya hər sorğu üçün default seçim olmamalıdır.
        - Əvvəlki sorğunun destinasiya seçimini yeni sorğuya avtomatik tətbiq etmə.
        - Büdcə real olmalıdır: məbləğ destinasiyanın qiymət səviyyəsinə uyğun
          gəlmirsə, daha əlverişli yer seç.


        PLANIN MƏZMUNU
        - Otel tipini büdcə xülasəsindəki "accommodation" məbləğinə uyğunlaşdır.
        - Yemək üstünlüyünü "food" məbləğinə və yemək məkanlarının seçiminə tətbiq et.
        - Səyahətin məqsədi seçilən fəaliyyətlərin xarakterini müəyyən etsin.
        - Hər gün üçün vaxt ardıcıllığı məntiqli olmalıdır: yol vaxtı, istirahət
          və yemək nəzərə alınmalıdır.
        - Fəaliyyətlər istifadəçinin maraqları və seçilmiş destinasiya ilə uyğun
          olmalıdır.


        DİL
        - Bütün istifadəçiyə görünən mətnlər Azərbaycan dilində olmalıdır.
        - "title" və "description" Azərbaycan dilində yazılmalıdır.
        - Xüsusi adlar (otel, hava limanı, muzey, abidə və s.) orijinal
          yazılışında qala bilər, lakin cümlənin qalan hissəsi Azərbaycan
          dilində olmalıdır.
        - "category" texniki enum dəyəridir və tərcümə olunmur:
          TRANSPORT, ACCOMMODATION, FOOD, ACTIVITIES.
        - "time" 24 saatlıq formatda olmalıdır, məsələn "09:00".


        "destination" SAHƏSİ — DİL QAYDASINDAN İSTİSNADIR
        - Yalnız məkanın adı yazılmalıdır.
        - Məkan adı ingilis/Latin orijinal formasında saxlanmalıdır.
        - Destination tərcümə edilməməlidir.
        - Destination-a ölkə adı əlavə edilməməlidir.
        - Vergül, tire, təsvir və ya başqa əlavə yazılmamalıdır.
        - Bu dəyər şəkil axtarışında açar söz kimi istifadə olunur.
        """;


    // additionalProperties:false hər obyekt səviyyəsində — OpenAI-nin "strict" structured
    // output rejimi bunu tələb edir (əks halda schema tam məcburi olmur).
    private static final String OUTPUT_SCHEMA_JSON = """
            {
              "type": "object",
              "properties": {
                "destination": { "type": "string" },
                "days": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "dayNumber": { "type": "integer" },
                      "items": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "properties": {
                            "time": { "type": "string" },
                            "title": { "type": "string" },
                            "description": { "type": "string" },
                            "category": {
                              "type": "string",
                              "enum": [
                                "TRANSPORT",
                                "ACCOMMODATION",
                                "FOOD",
                                "ACTIVITIES"
                              ]
                            }
                          },
                          "required": ["time", "title", "description", "category"],
                          "additionalProperties": false
                        }
                      }
                    },
                    "required": ["dayNumber", "items"],
                    "additionalProperties": false
                  }
                },
                "budgetSummary": {
                  "type": "object",
                  "properties": {
                    "accommodation": { "type": "number" },
                    "food": { "type": "number" },
                    "transport": { "type": "number" },
                    "activities": { "type": "number" },
                    "total": { "type": "number" }
                  },
                  "required": ["accommodation", "food", "transport", "activities", "total"],
                  "additionalProperties": false
                }
              },
              "required": ["destination", "days", "budgetSummary"],
              "additionalProperties": false
            }
            """;

    private final ObjectMapper objectMapper;

    String buildUserMessage(AiRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Maraqlar: ").append(formatInterests(request.interests())).append("\n");
        sb.append("Yoldaş: ").append(request.companion()).append("\n");

        if (request.familyDetails() != null) {
            FamilyDetails f = request.familyDetails();
            sb.append("Ailə tərkibi: ").append(f.adults()).append(" böyük, ")
                    .append(f.children()).append(" uşaq\n");
        }

        sb.append("Büdcə: ").append(formatBudget(request.budget())).append("\n");
        sb.append("Tarix: ").append(formatDates(request.dates())).append("\n");

        if (request.hotelType() != null) {
            sb.append("Otel tipi: ").append(request.hotelType()).append("\n");
        }
        if (request.mealPreference() != null) {
            sb.append("Yemək üstünlüyü: ").append(request.mealPreference()).append("\n");
        }
        if (request.tripPurpose() != null && !request.tripPurpose().isEmpty()) {
            sb.append("Səyahətin məqsədi: ")
                    .append(request.tripPurpose().stream().map(Enum::name).collect(Collectors.joining(", ")))
                    .append("\n");
        }

        return sb.toString();
    }

    Map<String, Object> outputSchema() {
        try {
            return objectMapper.readValue(OUTPUT_SCHEMA_JSON, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("AI output schema parse olunmadı", e);
        }
    }

    private String formatInterests(Set<InterestType> interests) {
        return interests.stream().map(Enum::name).collect(Collectors.joining(", "));
    }

    private String formatBudget(Budget budget) {
        // Frontend həmişə tier göndərir (CUSTOM halında da) — exactAmount yoxsa cavab
        // itərdi, ona görə CUSTOM dəyəri həmişə büdcə xülasəsinə yazılır.
        if (budget.tier() == BudgetTier.CUSTOM && budget.exactAmount() != null) {
            return "CUSTOM — dəqiq məbləğ: " + budget.exactAmount();
        }
        return budget.tier() == null
                ? "dəqiq məbləğ: " + budget.exactAmount()
                : budget.tier().name();
    }

    private String formatDates(TravelDates dates) {
        if (dates.startDate() != null) {
            return dates.startDate() + " — " + dates.endDate();
        }
        return "təxmini müddət: " + dates.approximateDuration();
    }
}