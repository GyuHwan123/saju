import { Link } from "react-router-dom";

export default function Header() {
    return (
        <header className="header">
            <h2>사주 프로젝트</h2>

            <nav
                style={{
                    display: "flex",
                    gap: "20px",
                    marginTop: "10px",
                }}
            >
                <Link to="/">홈</Link>
                <Link to="/login">로그인</Link>
            </nav>
        </header>
    );
}