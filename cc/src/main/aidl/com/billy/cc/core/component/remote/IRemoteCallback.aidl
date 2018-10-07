// com.billy.core.component.IProcessCrossCCService.aidl
package com.billy.cc.core.component.remote;

// Declare any non-default types here with import statements
import com.billy.cc.core.component.remote.RemoteCCResult;

interface IRemoteCallback {

    void callback(in RemoteCCResult remoteCCResult);

}
