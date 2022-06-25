import axios from 'axios';
import { showNotification } from '@mantine/notifications';
import { clearJwt } from './auth.service';

axios.interceptors.request.use(function (request) {
    // @ts-ignore
    request.headers.common.Authorization = `Bearer ${localStorage.getItem('user')}`;
    return request;
  }, function (error) {
    // Do something with request error
    return Promise.reject(error);
  });

axios.interceptors.response.use(function (response) {
  return response;
}, function (error) {
  if (error?.response?.status === 401) {
    showNotification({
      id: 'authentication-error',
      color: 'red',
      title: 'Authentication error',
      message: 'You have been logged out, as Universal Media Server could not be reached to re-authenticate. Please click here to log in again.',
      autoClose: false,
      onClick: redirectToLogin,
    });
  }
  return Promise.reject(error);
});

const redirectToLogin = () => {
  clearJwt();
  window.location.href = '/login';
};
