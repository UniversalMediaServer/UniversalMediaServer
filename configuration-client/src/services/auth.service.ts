import axios from 'axios';
import jwt_decode from 'jwt-decode';
import _ from 'lodash';

const storeJwtInLocalStorage = (jwt: string) => {
  localStorage.setItem('user', jwt);
  // @ts-ignore
  const {exp} = jwt_decode(jwt);
  localStorage.setItem('tokenExpiry', exp);
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

export const changePassword = async (currentPassword: string, newPassword: string) => {
  try {
    const response = await axios
      .post('/v1/api/user/changepassword', {
        password: currentPassword,
        newPassword,
      });
    return response.data;
  } catch (e: unknown) {
    const { error } = _.get(e, 'response.data');
    if (error) {
      return { error };
    }
    throw e;
  }
}

export const refreshToken = async () => {
  const response = await axios
    .post('/v1/api/auth/refresh', {});
  if (response.data.token) {
    storeJwtInLocalStorage(response.data.token);
  }
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

export const getJwt = () => {
  return localStorage.getItem('user');
}

export const getJwtPayload = () => {
  return localStorage.getItem('user')?.split('.')[1];
}
