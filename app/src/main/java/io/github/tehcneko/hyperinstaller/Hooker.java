package io.github.tehcneko.hyperinstaller;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam;
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam;

@SuppressLint("PrivateApi")
public class Hooker extends XposedModule {

    private static final String TAG = "HyperInstaller";
    private static boolean fakeCTS = false;

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        // no-op, initialization not needed
    }

    @Override
    public void onSystemServerStarting(@NonNull SystemServerStartingParam param) {
        try {
            hookPackageManagerServiceImpl(param.getClassLoader());
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "Failed to hook PackageManagerServiceImpl", t);
        }
    }

    private void hookPackageManagerServiceImpl(ClassLoader classLoader) throws ClassNotFoundException {
        var cls = classLoader.loadClass("com.android.server.pm.PackageManagerServiceImpl");
        for (var method : cls.getDeclaredMethods()) {
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
}
