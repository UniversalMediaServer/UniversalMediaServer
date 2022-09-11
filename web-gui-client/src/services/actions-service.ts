import axios from 'axios';
import { actionsApiUrl } from '../utils';

const PERMITTED_ACTIONS = [
  'Process.Exit',
  'Process.Reboot',
  'Process.Reboot.Trace',
  'Server.ResetCache',
  'Server.Restart',
  'Server.ScanAllSharedFolders',
  'Server.ScanAllSharedFoldersCancel',
  'Server.ScanAllSharedFoldersCancel',
];

export const sendAction = async(operation: string) => {
  if (PERMITTED_ACTIONS.includes(operation)) {
    return axios
      .post(actionsApiUrl, {
        operation,
      })
      .then((response) => {
        return response.data;
      });
  };
}
