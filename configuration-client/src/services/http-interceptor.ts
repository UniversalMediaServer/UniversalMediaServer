import axios from 'axios';

axios.interceptors.request.use(function (request) {
    // @ts-ignore
    request.headers.common.Authorization = `Bearer ${localStorage.getItem('user')}`;
    // @ts-ignore
    console.log(`Set auth header to ${request.headers.common.Authorization}`)
    return request;
  }, function (error) {
    // Do something with request error
    return Promise.reject(error);
  });