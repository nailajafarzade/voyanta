import bangkok1 from "../assets/bangkok1.jpg";
import bangkok2 from "../assets/bangkok2.jpg";
import bangkok3 from "../assets/bangkok3.jpg";
import bangkok4 from "../assets/bangkok4.jpg";
import paris1 from "../assets/paris1.jpg";
import paris2 from "../assets/paris2.jpg";
import paris3 from "../assets/paris3.jpg";
import paris4 from "../assets/paris4.jpg";
import dubai1 from "../assets/dubai1.jpg";
import dubai2 from "../assets/dubai2.jpg";
import dubai3 from "../assets/dubai3.jpg";
import dubai4 from "../assets/dubai4.jpg";
import london2 from "../assets/london2.jpg";
import london3 from "../assets/london3.jpg";
import london4 from "../assets/london4.jpg";

// NOTE: disk file is `london1.png` (not .jpg). Files must not be renamed,
// so the existing filename is imported exactly.
import london1 from "../assets/london1.png";

/**
 * Static "Most Loved Places" data — local images only.
 * No Unsplash, no API. 4 destinations x 4 images each.
 */
export const LOVED_PLACES = [
  {
    id: "bangkok",
    name: "Bangkok",
    country: "Tayland",
    flag: "🇹🇭",
    annualVisitors: "~30 milyon / il",
    about: "Bangkok Taylandın paytaxtı və ölkənin əsas mədəni, turizm və iqtisadi mərkəzlərindən biridir.",
    whyPeopleChoose: "Məbədləri, küçə yeməkləri, gecə həyatı, alış-veriş imkanları və zəngin Tay mədəniyyəti ilə məşhurdur.",
    popularPlaces: ["Grand Palace", "Wat Arun", "Wat Pho", "Chatuchak Market"],
    images: [bangkok1, bangkok2, bangkok3, bangkok4],
  },
  {
    id: "paris",
    name: "Paris",
    country: "Fransa",
    flag: "🇫🇷",
    annualVisitors: "~36 milyon / il",
    about: "Paris Fransanın paytaxtı və dünyanın ən mühüm incəsənət, mədəniyyət, moda və turizm mərkəzlərindən biridir.",
    whyPeopleChoose: "Eyfel qülləsi, incəsənət, moda, memarlıq və romantik atmosferi ilə dünyanın ən məşhur şəhərlərindən biridir.",
    popularPlaces: ["Eiffel Tower", "Louvre Museum", "Notre-Dame", "Arc de Triomphe"],
    images: [paris1, paris2, paris3, paris4],
  },
  {
    id: "dubai",
    name: "Dubai",
    country: "Birləşmiş Ərəb Əmirlikləri",
    flag: "🇦🇪",
    annualVisitors: "~20 milyon / il",
    about: "Dubai müasir memarlığı, turizm infrastrukturu və beynəlxalq şəhər həyatı ilə dünyanın ən məşhur səyahət istiqamətlərindən biridir.",
    whyPeopleChoose: "Müasir memarlığı, Burj Khalifa, lüks alış-veriş mərkəzləri, çimərlikləri və səhrası ilə məşhurdur.",
    popularPlaces: ["Burj Khalifa", "Dubai Marina", "Palm Jumeirah", "Dubai Mall"],
    images: [dubai1, dubai2, dubai3, dubai4],
  },
  {
    id: "london",
    name: "London",
    country: "Birləşmiş Krallıq",
    flag: "🇬🇧",
    annualVisitors: "~21 milyon / il",
    about: "London Birləşmiş Krallığın paytaxtı və dünyanın ən böyük mədəniyyət, tarix, biznes və turizm mərkəzlərindən biridir.",
    whyPeopleChoose: "Tarixi abidələri, muzeyləri, kral irsi, məşhur görməli yerləri və çoxmədəniyyətli şəhər həyatı ilə seçilir.",
    popularPlaces: ["Big Ben", "Tower Bridge", "Buckingham Palace", "London Eye"],
    images: [london1, london2, london3, london4],
  },
];

export default LOVED_PLACES;
