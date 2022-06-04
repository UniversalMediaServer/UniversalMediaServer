import axios from "axios";
const API_URL = "http://localhost:8080/v1/api/auth/";
export const register = (username: string, password: string) => {
  return axios.post(API_URL + "signup", {
    username,
    password,
  });
};
export const login = (username: string, password: string) => {
  return axios
    .post(API_URL + "signin", {
      username,
      password,
    })
    .then((response) => {
      console.log(response)
      if (response.data.accessToken) {
        localStorage.setItem("user", JSON.stringify(response.data));
      }
      return response.data;
    });
};
export const logout = () => {
  localStorage.removeItem("user");
};
export const getCurrentUser = () => {
  const userStr = localStorage.getItem("user");
  if (userStr) return JSON.parse(userStr);
  return null;
};