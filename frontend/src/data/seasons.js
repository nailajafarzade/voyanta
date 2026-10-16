import springAmsterdam from "../assets/spring-amsterdam.jpg";
import springKyoto from "../assets/spring-kyoto.jpg";
import springSeoul from "../assets/spring-seoul.jpg";
import springWashingtonDc from "../assets/spring-washington-dc.jpg";
import summerAmalfiCoast from "../assets/summer-amalfi-coast.jpg";
import summerSantorini from "../assets/summer-santorini.jpg";
import summerBali from "../assets/summer-bali.jpg";
import summerBarcelona from "../assets/summer-barcelona.jpg";
import autumnDolomites from "../assets/autumn-dolomites.jpg";
import autumnPrague from "../assets/autumn-prague.jpg";
import autumnNewYork from "../assets/autumn-new-york.jpg";
import autumnIstanbul from "../assets/autumn-istanbul.jpg";
import winterLapland from "../assets/winter-lapland.jpg";
import winterZermatt from "../assets/winter-zermatt.jpg";
import winterReykjavik from "../assets/winter--reykjavik.jpg";
import winterVienna from "../assets/winter-vienna.jpg";

// Disk file is `winter--reykjavik.jpg` (double dash). Task text says
// `winter-reykjavik.jpg`, but files must not be renamed/moved, so we
// import the existing filename exactly.

/**
 * Static Seasons data — local images only (no Unsplash, no backend).
 * UI renders tabs + cards from this file; no JSX duplication per season.
 */
export const SEASONS = [
  {
    key: "spring",
    label: "Spring",
    azLabel: "Yaz",
    icon: "🌸",
    blurb: "Lalələr, gilas çiçəkləri və mülayim günlər.",
    destinations: [
      {
        id: "spring-amsterdam",
        name: "Amsterdam",
        country: "Niderland",
        flag: "🇳🇱",
        image: springAmsterdam,
        description: "Lalələr, kanallar, velosiped sürmək və mülayim yaz havası.",
        whySeason: "Yaz lalələri, çiçək açan parkları və kanal kənarlarının yaşıllığını gətirir.",
        bestMonths: "Mart–May",
        highlights: ["Lalələr", "Kanallar", "Velosiped", "Parklar"],
      },
      {
        id: "spring-kyoto",
        name: "Kioto",
        country: "Yaponiya",
        flag: "🇯🇵",
        image: springKyoto,
        description: "Gilas çiçəkləri, tarixi məbədlər və ənənəvi küçələr.",
        whySeason: "Sakura mövsümü və məbədlərin, qədim küçələrin ətrafında hanami.",
        bestMonths: "Mart sonu–aprel əvvəli",
        highlights: ["Gilas çiçəkləri", "Məbədlər", "Hanami", "Ənənəvi küçələr"],
      },
      {
        id: "spring-seoul",
        name: "Seul",
        country: "Cənubi Koreya",
        flag: "🇰🇷",
        image: springSeoul,
        description: "Gilas çiçəkləri, müasir şəhər həyatı və yaz parkları.",
        whySeason: "Çiçəklənmə mövsümü müasir şəhər mənzərəsini parklar və küçələrlə əhatə edir.",
        bestMonths: "Mart–aprel",
        highlights: ["Gilas çiçəkləri", "Parklar", "Müasir şəhər", "Yaz gəzintiləri"],
      },
      {
        id: "spring-washington-dc",
        name: "Vaşinqton, D.C.",
        country: "ABŞ",
        flag: "🇺🇸",
        image: springWashingtonDc,
        description: "Gilas çiçəkləri, abidələr, muzeylər və yaz gəzintiləri.",
        whySeason: "Tidal Basin və National Mall ətrafındakı gilas çiçəkləri ilə məşhurdur.",
        bestMonths: "Mart sonu–aprel əvvəli",
        highlights: ["Gilas çiçəkləri", "Tidal Basin", "National Mall", "Abidələr"],
      },
    ],
  },
  {
    key: "summer",
    label: "Summer",
    azLabel: "Yay",
    icon: "☀️",
    blurb: "Günəşli sahillər, adalar və uzun Aralıq dənizi günləri.",
    destinations: [
      {
        id: "summer-amalfi-coast",
        name: "Amalfi sahili",
        country: "İtaliya",
        flag: "🇮🇹",
        image: summerAmalfiCoast,
        description: "Təsirli sahil mənzərələri, rəngarəng kəndlər və Aralıq dənizi çimərlikləri.",
        whySeason: "İsti hava sahil xətti, çimərliklər və dənizkənarı şəhərciklər üçün idealdır.",
        bestMonths: "İyun–sentyabr",
        highlights: ["Amalfi sahili", "Pozitano", "Aralıq dənizi", "Sahil mənzərələri"],
      },
      {
        id: "summer-santorini",
        name: "Santorini",
        country: "Yunanıstan",
        flag: "🇬🇷",
        image: summerSantorini,
        description: "Ağ rəngli kəndlər, mavi günbəzlər və Egey dənizi mənzərələri.",
        whySeason: "Günəşli uzun günlər çimərliklər, gün batımları və sahil mənzərələri üçün idealdır.",
        bestMonths: "İyun–sentyabr",
        highlights: ["Mavi günbəzli binalar", "Kaldera", "Gün batımı mənzərələri", "Egey dənizi"],
      },
      {
        id: "summer-bali",
        name: "Bali",
        country: "İndoneziya",
        flag: "🇮🇩",
        image: summerBali,
        description: "Tropik çimərliklər, yamyaşıl landşaftlar, məbədlər və ada mədəniyyəti.",
        whySeason: "Quraq mövsüm çimərliklər, açıq havada vaxt keçirmək və kəşf etmək üçün günəş gətirir.",
        bestMonths: "İyun–avqust",
        highlights: ["Çimərliklər", "Məbədlər", "Düyü terrasları", "Tropik təbiət"],
      },
      {
        id: "summer-barcelona",
        name: "Barselona",
        country: "İspaniya",
        flag: "🇪🇸",
        image: summerBarcelona,
        description: "Aralıq dənizi çimərlikləri, memarlıq, yeməklər və şəhər həyatı.",
        whySeason: "İsti hava çimərlik istirahətini memarlıq gəzintiləri ilə birləşdirir.",
        bestMonths: "İyun–sentyabr",
        highlights: ["Aralıq dənizi çimərlikləri", "Memarlıq", "Yeməklər", "Şəhər gəzintiləri"],
      },
    ],
  },
  {
    key: "autumn",
    label: "Autumn",
    azLabel: "Payız",
    icon: "🍂",
    blurb: "Qızılı vadilər, tarixi küçələr və sakit şəhər gəzintiləri.",
    destinations: [
      {
        id: "autumn-dolomites",
        name: "Dolomit dağları",
        country: "İtaliya",
        flag: "🇮🇹",
        image: autumnDolomites,
        description: "Əzəmətli dağlar, alp kəndləri və payız rəngləri.",
        whySeason: "Vadilər qızılı tonlara bürünür, dağ mənzərələri isə yenə də möhtəşəm qalır.",
        bestMonths: "Sentyabr–oktyabr",
        highlights: ["Dağlar", "Payız rəngləri", "Alp kəndləri", "Piyada turizm"],
      },
      {
        id: "autumn-prague",
        name: "Praqa",
        country: "Çexiya",
        flag: "🇨🇿",
        image: autumnPrague,
        description: "Tarixi memarlıq, füsunkar küçələr və payız mənzərələri.",
        whySeason: "Sərin hava və payız yarpaqları tarixi mərkəzdə sakit gəzintilər üçün şərait yaradır.",
        bestMonths: "Sentyabr–oktyabr",
        highlights: ["Köhnə şəhər", "Tarixi memarlıq", "Çarlz körpüsü", "Payız gəzintiləri"],
      },
      {
        id: "autumn-new-york",
        name: "Nyu-York",
        country: "ABŞ",
        flag: "🇺🇸",
        image: autumnNewYork,
        description: "Məşhur görməli yerlər, müxtəlif məhəllələr və payız parkları.",
        whySeason: "Payız yarpaqları, xüsusilə Mərkəzi Parkda, şəhəri rəngləndirir.",
        bestMonths: "Sentyabr–noyabr",
        highlights: ["Mərkəzi Park", "Manhetten", "Şəhər siluet̶i", "Payız yarpaqları"],
      },
      {
        id: "autumn-istanbul",
        name: "İstanbul",
        country: "Türkiyə",
        flag: "🇹🇷",
        image: autumnIstanbul,
        description: "Tarixi abidələr, Bosfor mənzərələri və zəngin irs.",
        whySeason: "Mülayim temperatur tarixi yerləri, küçələri və sahil boyunu gəzmək üçün əlverişlidir.",
        bestMonths: "Sentyabr–noyabr",
        highlights: ["Bosfor", "Ayasofya", "Tarixi küçələr", "Türk mətbəxi"],
      },
    ],
  },
  {
    key: "winter",
    label: "Winter",
    azLabel: "Qış",
    icon: "❄️",
    blurb: "Qarlı dağlar, şimal işıqları və qış şəhərləri.",
    destinations: [
      {
        id: "winter-lapland",
        name: "Laplandiya",
        country: "Finlandiya",
        flag: "🇫🇮",
        image: winterLapland,
        description: "Qarlı landşaftlar, Arktika təcrübələri və şimal işıqları.",
        whySeason: "Klassik qarlı mənzərələr, Arktika fəaliyyətləri və aurora gecələri.",
        bestMonths: "Dekabr–mart",
        highlights: ["Şimal işıqları", "Qar", "Arktika fəaliyyətləri", "Qış kottecləri"],
      },
      {
        id: "winter-zermatt",
        name: "Sermatt",
        country: "İsveçrə",
        flag: "🇨🇭",
        image: winterZermatt,
        description: "Alp mənzərələri, qarlı dağlar və Materhorn.",
        whySeason: "Qarlı zirvələr, xizək sürmək və alp istirahəti üçün əla şərait.",
        bestMonths: "Dekabr–mart",
        highlights: ["Materhorn", "Xizək sürmək", "Alp kəndləri", "Qarlı dağlar"],
      },
      {
        id: "winter-reykjavik",
        name: "Reykyavik",
        country: "İslandiya",
        flag: "🇮🇸",
        image: winterReykjavik,
        description: "Şimal işıqları, vulkanik landşaftlar və geotermal hovuzlar.",
        whySeason: "Aurora üçün uzun gecələr və soyuq təbiətin fonunda isti hovuzlar.",
        bestMonths: "Noyabr–mart",
        highlights: ["Şimal işıqları", "Geotermal hovuzlar", "Qış landşaftları", "İslandiya təbiəti"],
      },
      {
        id: "winter-vienna",
        name: "Vyana",
        country: "Avstriya",
        flag: "🇦🇹",
        image: winterVienna,
        description: "Tarixi memarlıq, kafelər və qış atmosferi.",
        whySeason: "Mövsümi bazarlar və tarixi küçələr klassik qış əhval-ruhiyyəsi yaradır.",
        bestMonths: "Noyabr–yanvar",
        highlights: ["Milad bazarları", "Tarixi memarlıq", "Kafelər", "Klassik mədəniyyət"],
      },
    ],
  },
];

export default SEASONS;