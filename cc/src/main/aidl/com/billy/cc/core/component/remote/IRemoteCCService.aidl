// com.billy.core.component.IProcessCrossCCService.aidl
package com.billy.cc.core.component.remote;

// Declare any non-default types here with import statements
import com.billy.cc.core.component.remote.RemoteCC;
import com.billy.cc.core.component.remote.RemoteCCResult;

interface IRemoteCCService {

    RemoteCCResult call(in RemoteCC remoteCC);

    void cancel(String callId);

    void timeout(String callId);

    String getComponentProcessName(String componentName);
}
