import axios from 'axios';

export const login = (username: string, password: string) => {
  return axios
    .post('/v1/api/login', {
      username,
      password,
    })
    .then((response) => {
      console.log(response)
      if (response.data.token) {
        localStorage.setItem('user', response.data.token);
      }
      return response.data;
    });
};
