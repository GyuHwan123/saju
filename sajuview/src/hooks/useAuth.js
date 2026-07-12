import { useContext } from "react";
import { AuthContext } from "../contexts/AuthProvider";

export default function useAuth() {
    const context = useContext(AuthContext);

    if (!context) {
        throw new Error("useAuth는 AuthProvider 내부에서 사용해야 합니다.");
    }

    return context;
}