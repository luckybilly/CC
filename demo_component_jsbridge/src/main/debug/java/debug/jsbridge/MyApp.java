package debug.jsbridge;

import android.app.Application;

import com.billy.cc.core.component.CC;

/**
 * @author billy.qi
 * @since 18/9/15 10:38
 */
public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CC.enableVerboseLog(true);
        CC.enableDebug(true);
        CC.enableRemoteCC(true);
    }
}
