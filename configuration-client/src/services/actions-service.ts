import axios from 'axios';

const PERMITTED_ACTIONS = [
  'Process.Reboot.Trace',
  'Server.ResetCache',
  'Server.Restart',
];

export const sendAction = async(operation: string) => {
  if (PERMITTED_ACTIONS.includes(operation)) {
    return axios
      .post('/v1/api/actions/', {
        operation,
      })
      .then((response) => {
        return response.data;
      });
  };
}
