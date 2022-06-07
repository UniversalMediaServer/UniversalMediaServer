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

export const changePassword = (password: string) => {
  return axios
    .post('/v1/api/user/changepassword', {
      password,
    })
    .then((response) => {
      return response.data;
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
  console.log('refreshAuthTokenNearExpiry')
  const FIVE_SECONDS_IN_MS = 5000;
  const exp = Number(localStorage.getItem('tokenExpiry'));

  const now = Math.floor(new Date().getTime() / 1000);
  const refreshInterval = (exp - now) * 1000 - FIVE_SECONDS_IN_MS;
  setTimeout(async() => {
    console.log('in settimeout');
    await refreshToken()
  }, refreshInterval);
}
