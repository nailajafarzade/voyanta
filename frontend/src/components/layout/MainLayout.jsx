import Header from "./Header";
import { Outlet, useLocation } from "react-router-dom";
import Footer from "./Footer";

function MainLayout() {
  const { pathname } = useLocation();

  // İstiqamət səhifəsi öz hero-su ilə tam hündürlükdədir — boşluq lazım deyil
  const isDestinationPage = pathname.startsWith("/destination/");

  return (
    <>
      <Header />

      <main className={isDestinationPage ? "" : "pt-24 sm:pt-28"}>
        <Outlet />
      </main>

      <Footer />
    </>
  );
}

export default MainLayout;
