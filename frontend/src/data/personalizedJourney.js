import tromsoImage from "../assets/tromso.jpg";
import capetownImage from "../assets/capetown.jpg";
import cuscoImage from "../assets/cusco.jpg";
import sydneyImage from "../assets/sydney.jpg";
import mexicoImage from "../assets/mexico.jpg";
import lisbonImage from "../assets/lisbon.jpg";
import reykjavikImage from "../assets/reykjavik.jpg";
import pragueImage from "../assets/prague.jpg";
import zanzibarImage from "../assets/zanzibar.jpg";
import queenstownImage from "../assets/queenstown.jpg";
import marrakechImage from "../assets/marrakech.jpg";

/**
 * "Sənə özəl səyahət marşrutu" — 100% lokal, statik data.
 * Heç bir API sorğusu yoxdur, backend-dən asılı deyil.
 * Bütün görünən mətnlər Azərbaycanca.
 */
export const PERSONALIZED_JOURNEY = [
  {
    id: "tromso",
    city: "Tromsø",
    country: "Norveç",
    image: tromsoImage,
    alt: "Tromsø, Norveç — şimal işıqları altında qarlı mənzərə",
    headline: "Bir az möcüzə axtarırsan?",
    description: "Şimal işıqları, qarlı mənzərələr və tamamilə fərqli hiss etdirən qış səyahəti.",
    tags: ["Şimal işıqları", "Qış", "Təbiət"],
  },
  {
    id: "capetown",
    city: "Keyptaun",
    country: "Cənubi Afrika",
    image: capetownImage,
    alt: "Keyptaun, Cənubi Afrika — okean və dağ mənzərəsi",
    headline: "Okean və açıq səma sənlikdir?",
    description: "Möhtəşəm dağların okeanla qovuşduğu, enerjisi heç vaxt bitməyən bir şəhər.",
    tags: ["Okean", "Təbiət", "Şəhər"],
  },
  {
    id: "cusco",
    city: "Kusko",
    country: "Peru",
    image: cuscoImage,
    alt: "Kusko, Peru — And dağları və qədim tarix",
    headline: "Adi marşrutlardan uzaqlaşmağa hazırsan?",
    description: "Qədim tarix, And dağları və unudulmaz macəralarla dolu bir səyahət.",
    tags: ["Tarix", "Dağlar", "Macəra"],
  },
  {
    id: "sydney",
    city: "Sidney",
    country: "Avstraliya",
    image: sydneyImage,
    alt: "Sidney, Avstraliya — çimərlik və şəhər mənzərəsi",
    headline: "Şəhər və dəniz birlikdə olsun istəyirsən?",
    description: "Çimərliklər, şəhər həyatı və möhtəşəm mənzərələr bir yerdə.",
    tags: ["Çimərlik", "Şəhər", "Okean"],
  },
  {
    id: "mexico",
    city: "Mexiko",
    country: "Meksika",
    image: mexicoImage,
    alt: "Mexiko, Meksika — rəngarəng küçələr və mədəniyyət",
    headline: "Yeni mədəniyyətlər kəşf etməyə hazırsan?",
    description: "Tarix, yeməklər, rəngarəng küçələr və canlı şəhər həyatı bir arada.",
    tags: ["Mədəniyyət", "Yemək", "Tarix"],
  },
  {
    id: "lisbon",
    city: "Lissabon",
    country: "Portuqaliya",
    image: lisbonImage,
    alt: "Lissabon, Portuqaliya — sahil və rəngarəng küçələr",
    headline: "Okean kənarında daha sakit günlər istəyirsən?",
    description: "Rəngarəng küçələr, sahil mənzərələri və saatlarla gəzmək istəyəcəyin bir şəhər.",
    tags: ["Şəhər", "Tarix", "Okean"],
  },
  {
    id: "reykjavik",
    city: "Reykyavik",
    country: "İslandiya",
    image: reykjavikImage,
    alt: "Reykyavik, İslandiya — geotermal təbiət və şimal işıqları",
    headline: "Tamamilə başqa bir dünya görmək istəyirsən?",
    description: "Geotermal mənzərələr, dramatik təbiət və şimal işıqlarını görmək fürsəti.",
    tags: ["Təbiət", "Şimal işıqları", "Geotermal"],
  },
  {
    id: "prague",
    city: "Praqa",
    country: "Çexiya",
    image: pragueImage,
    alt: "Praqa, Çexiya — tarixi küçələr və memarlıq",
    headline: "Keçmişin ab-havasını hiss etmək istəyirsən?",
    description: "Tarixi küçələr, möhtəşəm memarlıq və piyada kəşf etmək üçün ideal şəhər.",
    tags: ["Tarix", "Memarlıq", "Şəhər"],
  },
  {
    id: "zanzibar",
    city: "Zənzibar",
    country: "Tanzaniya",
    image: zanzibarImage,
    alt: "Zənzibar, Tanzaniya — firuzəyi sular və tropik çimərlik",
    headline: "Həqiqi bir qaçışa ehtiyacın var?",
    description: "Firuzəyi sular, tropik çimərliklər və Hind okeanının qoynunda sakit günlər.",
    tags: ["Çimərlik", "Tropik", "İstirahət"],
  },
  {
    id: "queenstown",
    city: "Kuinstoun",
    country: "Yeni Zelandiya",
    image: queenstownImage,
    alt: "Kuinstoun, Yeni Zelandiya — dağ havası və açıq təbiət",
    headline: "Bir az macəra axtarırsan?",
    description: "Dağ havası, vəhşi təbiət və açıq havada unudulmaz təcrübələr.",
    tags: ["Macəra", "Dağlar", "Outdoor"],
  },
  {
    id: "marrakech",
    city: "Mərrakeş",
    country: "Mərakeş",
    image: marrakechImage,
    alt: "Mərrakeş, Mərakeş — rəngarəng bazarlar və tarixi küçələr",
    headline: "Tamamilə fərqli bir atmosfer istəyirsən?",
    description: "Rəngarəng bazarlar, tarixi küçələr və başqa heç nəyə bənzəməyən atmosfer.",
    tags: ["Mədəniyyət", "Bazarlar", "Memarlıq"],
  },
];

export default PERSONALIZED_JOURNEY;
