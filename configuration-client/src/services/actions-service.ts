import axios from 'axios';

const PERMITTED_ACTIONS = [
  'Server.ResetCache',
  'Server.Restart',
  'Server.ScanAllSharedFolders',
  'Server.ScanAllSharedFoldersCancel',
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
