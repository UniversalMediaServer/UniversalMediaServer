import { showNotification } from '@mantine/notifications';
import axios from 'axios';
import { ReactNode, useEffect, useState } from 'react';
import { accountsContext, UmsAccounts } from '../contexts/accounts-context';

interface Props {
  children?: ReactNode
}

export const AccountsProvider = ({ children, ...props }: Props) =>{
  const [accounts, setAccounts] = useState({usersManage:false,groupsManage:false,users:[],groups:[]} as UmsAccounts)

  useEffect(() => {
    axios.get('/v1/api/account/accounts')
      .then(function (response: any) {
        setAccounts(response.data);
      })
      .catch(function (error: Error) {
        console.log(error);
        showNotification({
          id: 'data-loading',
          color: 'red',
          title: 'Error',
          message: 'Accounts was not received from the server.',
          autoClose: 3000,
        });
      });
  }, []);
  
  const { Provider } = accountsContext;
  return(
    <Provider value={accounts}>
      {children}
    </Provider>
  )
}