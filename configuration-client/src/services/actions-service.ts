import axios from 'axios';

const PERMITTED_ACTIONS = ['Server.Restart'];

export const sendAction = async(action: string) => {
    if (PERMITTED_ACTIONS.includes(action)) {
      return axios
        .post('/v1/api/actions', {
          operation: 'Server.Restart',
        })
        .then((response) => {
          return response.data;
        });
    };
  }