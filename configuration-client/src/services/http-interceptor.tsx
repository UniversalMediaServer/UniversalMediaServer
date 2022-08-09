import axios from 'axios';
import { showNotification } from '@mantine/notifications';
import { getJwt, redirectToLogin } from './auth-service';

axios.interceptors.request.use(function (request) {
  const jwt = getJwt();
  if (jwt && request !== undefined && request.headers !== undefined) {
    request.headers.Authorization = "Bearer " + jwt;
  }
  return request;
}, function (error) {
  // Do something with request error
  return Promise.reject(error);
});

axios.interceptors.response.use(function (response) {
  return response;
}, function (error) {
  if (error?.response?.status === 401 && error?.config?.url !== "/v1/api/auth/login") {
    showNotification({
      id: 'authentication-error',
      color: 'red',
      title: 'Authentication error',
      message: 'You have been logged out from Universal Media Server. Please click here to log in again.',
      autoClose: false,
      onClick: redirectToLogin,
    });
  }
  return Promise.reject(error);
});
