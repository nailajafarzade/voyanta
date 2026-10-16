# React + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react/README.md) uses [Babel](https://babeljs.io/) for Fast Refresh
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react-swc) uses [SWC](https://swc.rs/) for Fast Refresh

## Voyanta — backend ilə işə salmaq

```bash
npm install
npm run dev
```

Frontend `http://localhost:5173` ünvanında açılır. `/api/...` sorğuları Vite proxy ilə
`http://localhost:8080` ünvanındakı Spring Boot backend-ə yönləndirilir (`.env` və `vite.config.js`).

Əvvəlcədən lazımdır:
- Backend işləyir (Postgres və Redis daxil) və `application.yaml`-də Google client id-si təyin olunub.
- `.env`-dəki `VITE_GOOGLE_CLIENT_ID` backend-dəki `voyanta.oauth.google.client-id` ilə eynidir.
- Google Cloud Console-da OAuth Client ID üçün "Authorized JavaScript origins" siyahısında
  `http://localhost:5173` var.
