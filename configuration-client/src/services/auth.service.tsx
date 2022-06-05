import axios from 'axios';

export const login = (username: string, password: string) => {
  return axios
    .post('/v1/api/login', {
      username,
      password,
    })
    .then((response) => {
      if (response.data.token) {
        localStorage.setItem('user', response.data.token);
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
