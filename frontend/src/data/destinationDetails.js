/**
 * İstiqamət ətraflı məlumatı — YALNIZ frontend (PO tələbi #3).
 *
 * Backend `Destination` yalnız { id, name, country, imageUrl, tag } qaytarır,
 * ona görə "görüləcək yerlər / fəaliyyətlər / faydalı məlumat" məzmunu
 * burada, istiqamət adına görə saxlanılır. Bu, backend API-sini dəyişmir —
 * şəbəkə tələbi yalnız mövcud `GET /destinations/featured` çağırışıdır.
 *
 * Açar: kiçik hərflə `name` (bəzən `country` daxil olur).
 * Tapılmasa `tag` əsasında generik məlumat yaradılır (utils/destinationDetails.js).
 */

export const DESTINATION_DETAILS = {
  santorini: {
    headline: "Ağ evlər, mavi qübələr və Ege dənizinə açılan gün batımı.",
    summary:
      "Santorini — Vulkan adasının üzərində qurulmuş kiçik Yunanıstan adası. Qırmızı qayalıqlara yapışan ağ evlər, dənizə baxan qəs nöqtələri və dünyanın ən tanınmış gün batımlarından biri buradadır.",
    bestMonths: [5, 6, 9, 10],
    bestSeason: "May–İyn və sentyabr–oktyabr — həm isti, həm də daha sakin.",
    stayLength: "3–4 gün",
    budget: "Orta–yüksək",
    language: "Yunan dili",
    currency: "Avro (EUR)",
    timezone: "GMT+3",
    facts: [
      { label: "Ən yaxşı dövr", value: "May – Oktyabr" },
      { label: "Tövsiyə olunan gün", value: "3 – 4 gün" },
      { label: "Valyuta", value: "Avro (EUR)" },
      { label: "Dilmər", value: "Yunan dili" },
    ],
    places: [
      { name: "Oia qəs", note: "İncir dərəsi və gün batımı üçün ən tanınmış qəs." },
      { name: "Fira", note: "Adanın paytaxtı; mağazalar, muzeylər və Fira–Oia gəzisi." },
      { name: "Kımolos çimərliyi", note: "Qara qumlu sahil, ən yaxşı gün batımı çimərliklərindən biri." },
      { name: "Qırmızı Beach", note: "Kırmızı vulkan qayaları arasındakı unikal sahil." },
      { name: "Pyrgos qalası", note: "Adanın ən yüksək nöqtəsində qalan və panoramı mənzərə." },
      { name: "Abbotts Tarxaxa", note: "Gəzilməsi asan, orta əsr kəndi." },
    ],
    activities: [
      { icon: "🌅", label: "Gün batımını izləmək" },
      { icon: "⛵", label: "Katamaran qəzəşiyi" },
      { icon: "🏊", label: "Sualtı şnorkəl" },
      { icon: "🍷", label: "Mənzərəli şarab ziyaretəti" },
      { icon: "🥾", label: "Vulkan yolu yürüşü" },
      { icon: "🛵", label: "Fira–Oia yolu" },
    ],
    tips: [
      "Gün batımı üçün Oia-da yerləri gün batımından 1.5 saat əvvəl tut.",
      "Yayın əvvəlində qiymətlər qalxır — sentyabr ən yaxşı kompromisdir.",
      "Eşşəklərlə ehtiyat ol — qalxıb enmək yorucudur.",
    ],
  },

  maldiv: {
    headline: "Lacivert suyun üzərində villa və mərcan qayaları.",
    summary:
      "Maldiv adaları — Hind okeanında 1000-dən çox ada və atollardan ibarət kiçik dövlətdir. Su üstü villalar, şnorkəl üçün dünyanın ən yaxşı suları və tam təcrid edilmiş dincəlmə buradadır.",
    bestMonths: [11, 12, 1, 2, 3],
    bestSeason: "Noyabr–Mart — quru mövsümü, sular ən berrək olur.",
    stayLength: "5–7 gün",
    budget: "Yüksək",
    language: "Divehi dili, İngilis dili",
    currency: "Maldiv rupisi (MVR)",
    timezone: "GMT+5",
    facts: [
      { label: "Ən yaxşı dövr", value: "Noyabr – Mart" },
      { label: "Tövsiyə olunan gün", value: "5 – 7 gün" },
      { label: "Su temperaturı", value: "27–30 °C" },
      { label: "Valyuta", value: "Maldiv rupisi (MVR)" },
    ],
    places: [
      { name: "Malə", note: "Paytaxt; yerli bazar və mədəniyyət mərkəzi." },
      { name: "Vaadhoo", note: "Şnorkəl üçün ən tanınmış atollardan biri." },
      { name: "Baa Atol", note: "UNESCO bioşəhər; Hanifaru bayıqlar üçün ən yaxşı vaxtdır." },
      { name: "Ari Atol", note: "Sualtı park və yunus müşahidəsi imkanları." },
      { name: "Maafushi", note: "Yerli sakinlərlə qarşılıqlı təcrübə üçün ideal ada." },
    ],
    activities: [
      { icon: "🤿", label: "Sualtı şnorkəl" },
      { icon: "🐠", label: "Manta və köpək balığı" },
      { icon: "🏝️", label: "Su üstü tətil" },
      { icon: "💆", label: "Spa və masaj" },
      { icon: "🎣", label: "Balıq ovu" },
      { icon: "🛶", label: "Kayak və darts" },
    ],
    tips: [
      "Su üstü villalar üçün ən yaxşı şərtləri qış mövsümündə əldə edirsən.",
      "Mərcanlara toxunma — mühafizə qaydalarına əməl et.",
      "Hava dəyişkən ola bilər — bir neçə gün ehtiyat saxla.",
    ],
  },

  misir: {
    headline: "Piramidalar, mumiyalar və Nil üzərində 5000 illik tarix.",
    summary:
      "Misir — Nil çayının iki sahilində yayılan Antik Misir hökmdarlığı. Giza piramidalarından Karnak məbədinə qədər hər addım keçmiş əsrləri göstərir.",
    bestMonths: [10, 11, 12, 1, 2, 3],
    bestSeason: "Oktyabr–Aprel — isti, amma quru və cəlbedici hava.",
    stayLength: "5–7 gün",
    budget: "Orta",
    language: "Ərəb dili",
    currency: "Misir fundu (EGP)",
    timezone: "GMT+2",
    facts: [
      { label: "Ən yaxşı dövr", value: "Oktyabr – Aprel" },
      { label: "Tövsiyə olunan gün", value: "5 – 7 gün" },
      { label: "Valyuta", value: "Misir fundu (EGP)" },
      { label: "Dilmər", value: "Ərəb dili" },
    ],
    places: [
      { name: "Giza piramidaları", note: "Xufu, Xafirə və Menkaura; daxilə giriş mövcuddur." },
      { name: "Azadlıq Memorialı", note: "Yaxın əsrlərin ən böyük abidə kompleksi." },
      { name: "Karnak məbədi", note: "Dünyanın ən böyük açıq hava məbədi kompleksi." },
      { name: "Luxor məbədi", note: "Misirün ən qədim fəal məbədi." },
      { name: "Krallar vadisisi", note: "Firavunlara aid zəngin qəbir abidələri." },
      { name: "Khan el-Khalili", note: "Mısır ətirspazarlığının ən canlı bazarı." },
    ],
    activities: [
      { icon: "🐫", label: "Səhrada şam qaranlığı" },
      { icon: "🚢", label: "Nil kruizi" },
      { icon: "🛕", label: "Abaton və Yusif" },
      { icon: "🏛️", label: "Mumiya muzeyi" },
      { icon: "⛵", label: "Felyeks qəzəşiyi" },
      { icon: "🍫", label: "Kakao və çay ziyaretəti" },
    ],
    tips: [
      "Piramida qalxmaq üçün erkən səhər saatları daha rahatdır.",
      "Bəzən giriş haqqında əlavə ödəniş tələb olunur — əvvəlcədən dəqiqləşdir.",
      "Gözləyici ətəyin üçün örtü və su götür.",
    ],
  },

  "ismayıllı": {
    headline: "Quba–Xaçmaz sahili, meşəlik dağlar və isti bulaqlar.",
    summary:
      "İsmayıllı — Şimal sahilinin ən yaşıl bölgələrindən biri. Sahil, meşəlik dağlar, Xınalıq kimi UNESCO əhəmiyyətli yaşayış məntəqələri və isti su bələdələri buradadır.",
    bestMonths: [5, 6, 7, 8, 9],
    bestSeason: "May–Sentyabr — dəniz və meşədə rahat gəzinti havası.",
    stayLength: "2–3 gün",
    budget: "Aşağı–orta",
    language: "Azərbaycan dili",
    currency: "Azərbaycan manatı (AZN)",
    timezone: "GMT+4",
    facts: [
      { label: "Ən yaxşı dövr", value: "May – Sentyabr" },
      { label: "Tövsiyə olunan gün", value: "2 – 3 gün" },
      { label: "Valyuta", value: "Manat (AZN)" },
      { label: "Dilmər", value: "Azərbaycan dili" },
    ],
    places: [
      { name: "İstisuq", note: "Sahil və mineral su ehtiyatı bir yerdədir." },
      { name: "Qıvsalı qalası", note: "Dəniz ətrafında qurulmuş qala; piknik üçün əlverişli." },
      { name: "Xınalıq", note: "UNESCO əhəmiyyətli qəsəbə — sarı evlər və Xinalıqçay." },
      { name: "Quba şəhəri", note: "Xaçmaz istiqamətində bazar və qədim abidələr." },
      { name: "Gilan düzü", note: "Qarabağ dağlarına panoramı mənzərə." },
      { name: "Siyazan", note: "Sahil boyu məntəqə və çay plantasiyaları." },
    ],
    activities: [
      { icon: "🏔️", label: "Dağ yürüşləri" },
      { icon: "🌊", label: "Dəniz gəzintisi" },
      { icon: "🏕️", label: "Meşə pikniki" },
      { icon: "♨️", label: "İsti bulaqlar" },
      { icon: "📸", label: "Foto safari" },
      { icon: "🚗", label: "Dördbucaqlıq marşrut" },
    ],
    tips: [
      "Dağ yolları dar olur — diqqətli sür, sürəti azalt.",
      "Dənizə getmək üçün maşın deyil, avtobus da əlverişlidir.",
      "Ən rahat aylar may–sentyabr arasıdır.",
    ],
  },
};
