/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
import axios from 'axios';
import jwtDecode, { JwtPayload } from 'jwt-decode';
import { authApiUrl, playerApiUrl } from '../utils';

const storeJwtInLocalStorage = (jwt: string) => {
  axios.defaults.headers.common['Authorization'] = 'Bearer ' + jwt;
  localStorage.setItem('user', jwt);
  const decoded = jwtDecode<JwtPayload>(jwt);
  if (decoded.exp) {
    localStorage.setItem('tokenExpiry', decoded.exp.toString());
  }
}

export const login = async (username: string, password: string) => {
  const response = await axios
    .post(authApiUrl + 'login', {
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
}

export const create = async (username: string, password: string) => {
  const response = await axios
    .post(authApiUrl + 'create', {
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
}

export const disable = async () => {
  return await axios.get(authApiUrl + 'disable');
}

export const refreshToken = async () => {
  const response = await axios
    .post(authApiUrl + 'refresh', {});
  if (response.data.token) {
    storeJwtInLocalStorage(response.data.token);
  }
}

export const clearJwt = async () => {
  localStorage.removeItem('tokenExpiry');
  localStorage.removeItem('user');
  const uuid = sessionStorage.getItem('player');
  if (uuid) {
    try {
      await axios.post(playerApiUrl + 'logout', {uuid:uuid});
    } catch {/*server Forbidden or Unauthorized*/}
    sessionStorage.removeItem('player');
  }
  axios.defaults.headers.common['Authorization'] = undefined;
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

export const setAxiosAuthorization = () => {
  axios.defaults.headers.common['Authorization'] = localStorage.getItem('user') ? 'Bearer ' + localStorage.getItem('user') : undefined;
}

export const redirectToLogin = async () => {
  await clearJwt();
  window.location.reload();
}