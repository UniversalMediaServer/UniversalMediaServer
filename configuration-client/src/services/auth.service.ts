import axios from 'axios';
import jwt_decode from 'jwt-decode';

export const login = (username: string, password: string) => {
  return axios
    .post('/v1/api/auth/login', {
      username,
      password,
    })
    .then((response) => {
      if (response.data.token) {
        storeJwtInLocalStorage(response.data.token);
      }
      return response.data;
    });
};

export const changePassword = (currentPassword: string, newPassword: string) => {
  return axios
    .post('/v1/api/user/changepassword', {
      password: currentPassword,
      newPassword,
    })
    .then((response) => {
      return response.data;
    })
    .catch((e) => {
      const { error } = e.response.data
      if (error) {
        return {error};
      }
      throw e;
    });
}

export const refreshToken = () => {
  return axios
    .post('/v1/api/auth/refresh', {})
    .then((response) => {
      if (response.data.token) {
        storeJwtInLocalStorage(response.data.token);
      }
    });
}

const storeJwtInLocalStorage = (jwt: string) => {
  localStorage.setItem('user', jwt);
  // @ts-ignore
  const {exp} = jwt_decode(jwt);
  localStorage.setItem('tokenExpiry', exp);
}

export const refreshAuthTokenNearExpiry = () => {
  if (!localStorage.getItem('tokenExpiry')) {
    return;
  }
  const FIVE_SECONDS_IN_MS = 5000;
  const exp = Number(localStorage.getItem('tokenExpiry'));

  const now = Math.floor(new Date().getTime() / 1000);
  const refreshInterval = (exp - now) * 1000 - FIVE_SECONDS_IN_MS;
  setTimeout(async() => {
    await refreshToken();
  }, refreshInterval);
}

export const clearJwt = () => {
  localStorage.removeItem('tokenExpiry');
  localStorage.removeItem('user');
}
