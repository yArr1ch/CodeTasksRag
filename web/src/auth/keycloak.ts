import Keycloak from 'keycloak-js';

export const keycloak = new Keycloak({
    url: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8180',
    realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'algocoach',
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'algocoach-web'
});
