package io.github.tehcneko.hyperinstaller;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

@SuppressLint("PrivateApi")
public class Hooker extends XposedModule {

    private static final String TAG = "HyperInstaller";
    private static boolean fakeCTS = false;

    public Hooker(@NonNull XposedInterface base, @NonNull ModuleLoadedParam param) {
        super(base, param);
    }

    @Override
    public void onSystemServerLoaded(@NonNull SystemServerLoadedParam param) {
        try {
            hookPackageManagerServiceImpl(param.getClassLoader());
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Failed to hook PackageManagerServiceImpl", t);
        }
        try {
            hookIsCTS(param.getClassLoader());
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Failed to hook isCTS", t);
        }
    }

    private void hookPackageManagerServiceImpl(ClassLoader classLoader) throws ClassNotFoundException {
        var packageManagerServiceImpl = classLoader.loadClass("com.android.server.pm.PackageManagerServiceImpl");
        for (var method : packageManagerServiceImpl.getDeclaredMethods()) {
            var name = method.getName();
            if ("hookChooseBestActivity".equals(name) ||
                    "updateDefaultPkgInstallerLocked".equals(name) ||
                    "assertValidApkAndInstaller".equals(name)) {
                Log.d(TAG, "hooking method " + name);
                hook(method).intercept(chain -> {
                    fakeCTS = true;
                    try {
                        return chain.proceed();
                    } finally {
                        fakeCTS = false;
                    }
                });
                deoptimize(method);
            }
        }
    }

    private void hookIsCTS(ClassLoader classLoader) throws NoSuchMethodException, ClassNotFoundException {
        var cls = classLoader.loadClass("com.android.server.pm.PackageManagerServiceImpl");
        var isCTSMethod = cls.getDeclaredMethod("isCTS");
        hook(isCTSMethod).intercept(chain -> {
            if (fakeCTS) return true;
            return chain.proceed();
        });
    }
}
