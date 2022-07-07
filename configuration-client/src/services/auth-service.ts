import axios from 'axios';
import jwtDecode, { JwtPayload } from 'jwt-decode';

const storeJwtInLocalStorage = (jwt: string) => {
  localStorage.setItem('user', jwt);
  const decoded = jwtDecode<JwtPayload>(jwt);
  if (decoded.exp) {
    localStorage.setItem('tokenExpiry', decoded.exp.toString());
  }
}

export const login = async (username: string, password: string) => {
  const response = await axios
    .post('/v1/api/auth/login', {
      username,
      password,
    });
  if (response.data.token) {
    storeJwtInLocalStorage(response.data.token);
  }
  if (response.data.account) {
    //refresh session.account 
  }
  return response.data;
};

export const create = async (username: string, password: string) => {
  const response = await axios
    .post('/v1/api/auth/create', {
      username,
      password,
    });
  if (response.data.token) {
    storeJwtInLocalStorage(response.data.token);
  }
  if (response.data.account) {
    //refresh session.account 
  }
  return response.data;
};

export const disable = async () => {
  return await axios.get('/v1/api/auth/disable');
};

export const refreshToken = async () => {
  const response = await axios
    .post('/v1/api/auth/refresh', {});
  if (response.data.token) {
    storeJwtInLocalStorage(response.data.token);
  }
}

export const clearJwt = () => {
  localStorage.removeItem('tokenExpiry');
  localStorage.removeItem('user');
}

export const refreshAuthTokenNearExpiry = () => {
  if (!localStorage.getItem('tokenExpiry')) {
    return;
  }
  const FIVE_SECONDS_IN_MS = 5000;
  const exp = Number(localStorage.getItem('tokenExpiry'));

  const now = Math.floor(new Date().getTime() / 1000);
  const refreshInterval = (exp - now) * 1000 - FIVE_SECONDS_IN_MS;
  if (refreshInterval > 0) {
    setTimeout(async() => {
      await refreshToken();
    }, refreshInterval);
  } else {
    clearJwt();
  }
}

export const getJwt = () => {
  return localStorage.getItem('user');
}

export const redirectToLogin = () => {
  clearJwt();
  window.location.reload();
}