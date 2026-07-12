import { Outlet } from "react-router-dom";
import Header from "../components/Header";
import Footer from "../components/Footer";

export default function MainLayout() {
    return (
        <div className="layout">
            <Header />

            <main className="main">
                <Outlet />
            </main>

            <Footer />
        </div>
    );
}