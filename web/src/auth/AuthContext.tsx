import {createContext, useContext, useEffect, useMemo, useState} from 'react';
import {keycloak} from './keycloak';

type AuthContextValue = {
    ready: boolean;
    authenticated: boolean;
    admin: boolean;
    username?: string;
    login: () => void;
    logout: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

let keycloakInitialization: Promise<boolean> | undefined;

function initializeKeycloak() {
    keycloakInitialization ??= keycloak.init({
        onLoad: 'login-required',
        pkceMethod: 'S256',
        checkLoginIframe: false
    });
    return keycloakInitialization;
}

export function AuthProvider({children}: { children: React.ReactNode }) {
    const [ready, setReady] = useState(false);
    const [authenticated, setAuthenticated] = useState(false);

    useEffect(() => {
        initializeKeycloak().then(value => {
            setAuthenticated(value);
            setReady(true);
        }).catch(() => setReady(true));

        const refresh = window.setInterval(() => {
            if (keycloak.authenticated) {
                keycloak.updateToken(30).catch(() => keycloak.login());
            }
        }, 20_000);
        return () => window.clearInterval(refresh);
    }, []);

    const value = useMemo(() => ({
        ready,
        authenticated,
        admin: keycloak.hasRealmRole('ADMIN'),
        username: keycloak.tokenParsed?.preferred_username,
        login: () => keycloak.login(),
        logout: () => keycloak.logout({redirectUri: window.location.origin})
    }), [authenticated, ready]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
    const value = useContext(AuthContext);
    if (!value) {
        throw new Error('useAuth must be used inside AuthProvider');
    }
    return value;
}
