//
// Copyright (c) 2019-Present Syncari - All rights reserved.
//
import { useEffect } from 'react';

import { useEnhancedDispatch, useEnhancedSelector } from 'hooks/redux';
import usePreviousValue from 'hooks/usePreviousValue';
import { connectMessageStream } from 'store/app/actions';

const useConnectMessageStream = () => {
  const dispatch = useEnhancedDispatch();

  const currentInstanceSyncariId = useEnhancedSelector((state) => state.user.currentInstanceSyncariId);
  const previousCurrentInstanceSyncariId = usePreviousValue(currentInstanceSyncariId);

  const userEmail = useEnhancedSelector((state) => state.user.email);

  useEffect(() => {
    if (currentInstanceSyncariId && currentInstanceSyncariId !== previousCurrentInstanceSyncariId) {
      dispatch(connectMessageStream(currentInstanceSyncariId, userEmail));
    }
  }, [currentInstanceSyncariId, dispatch, previousCurrentInstanceSyncariId, userEmail]);
};

export default useConnectMessageStream;
