package uz.developer.privaterecorder.shell

import android.annotation.SuppressLint
import android.content.AttributionSource
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Process
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.Method

class WrappedShellContext(base: Context) : ContextWrapper(base) {

    companion object {
        private const val TAG = "WrappedShellContext"
        const val SHELL_PACKAGE_NAME = "com.android.shell"
        const val SHELL_UID = 2000

        @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
        fun create(): WrappedShellContext {
            // Bypass hidden API restrictions on Android 9–17
            try {
                HiddenApiBypass.addHiddenApiExemptions("L")
            } catch (e: Throwable) {
                Log.w(TAG, "HiddenApiBypass initialization warning: ${e.message}")
            }

            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentActivityThreadMethod: Method = activityThreadClass.getDeclaredMethod("currentActivityThread")
            currentActivityThreadMethod.isAccessible = true
            var activityThread = currentActivityThreadMethod.invoke(null)

            if (activityThread == null) {
                val systemMainMethod: Method = activityThreadClass.getDeclaredMethod("systemMain")
                systemMainMethod.isAccessible = true
                activityThread = systemMainMethod.invoke(null)
            }

            val getSystemContextMethod: Method = activityThreadClass.getDeclaredMethod("getSystemContext")
            getSystemContextMethod.isAccessible = true
            val systemContext = getSystemContextMethod.invoke(activityThread) as Context

            return WrappedShellContext(systemContext)
        }
    }

    override fun getPackageName(): String {
        return SHELL_PACKAGE_NAME
    }

    override fun getOpPackageName(): String {
        return SHELL_PACKAGE_NAME
    }

    fun getBasePackageName(): String {
        return SHELL_PACKAGE_NAME
    }

    override fun getAttributionSource(): AttributionSource {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AttributionSource.Builder(SHELL_UID)
                .setPackageName(SHELL_PACKAGE_NAME)
                .build()
        } else {
            super.getAttributionSource()
        }
    }
}
