package com.example.appdatacleanproject

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import java.io.File
import java.io.IOException


/**
 * 应用数据清除管理器。
 * 主要功能有清除内/外缓存，清除数据库，清除sharedPreference，清除files和清除自定义目录
 */
class AppDataCleanManager {
    companion object {
        /**
         * 方案一:利用命令行pm clear 包名，系统级别清除App数据
         *  优点:和任务管理器里面清除所有数据的操作一致，会删除所有的APP数据。重新进入APP还需重新申请权限。
         *  缺点:系统会直接杀掉APP进程，无法进行拉起APP的操作。
         * @param context
         * @return 是否清除成功
         */
        @JvmStatic
        fun commandLineClearData(context: Context): Boolean {
            Log.e("whh", "context.getPackageName()) = " + context.packageName)
            var flag = false
            try {
                if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) { // API >= 19，支持系统函数 clearApplicationUserData
                    val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager?
                    if (activityManager != null) {
                        flag = activityManager.clearApplicationUserData()
                        Log.e("whh", "activityManager.clearApplicationUserData() = " + activityManager.clearApplicationUserData())
                    }
                } else {
                    val p = execRuntimeProcess("pm clear " + context.packageName)
                    if (p != null) {
                        flag = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return flag
        }

        @JvmStatic
        private fun execRuntimeProcess(command: String?): Process? {
            var p: Process? = null
            try {
                p = Runtime.getRuntime().exec(command)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return p
        }

        /**
         * 方案二:手动删除内部储存和外部储存
         *
         * 优点:由自身控制，可以重新拉起APP。
         * 缺点:
         *  1.没有系统级的清除那么彻底，比如重新拉起后不需要重新申请权限。当然数据库，SharePreferences相关的通通都会删掉的
         *  2.因为数据库文件被删除，所以重新拉起启动页时需要删除当前进程，此时会黑屏一下。
         */
        @JvmStatic
        fun clearInternalExternalStorage(application: Application, activity: Activity, vararg filepath: String?) {
            clearInternalStorage(application)
            clearExternalStorage(application)
            Log.e("whh", "context.getCacheDir() = " + application.cacheDir.path)
            Log.e("whh", "context.getExternalCacheDir() = " + application.externalCacheDir!!.path)
            Log.e("whh", "context.Databases = " + "/data/data/" + application.packageName + "/databases")
            Log.e("whh", "context.SharedPreference = " + "/data/data/" + application.packageName + "/shared_prefs")
            Log.e("whh", "context.getFilesDir() = " + application.filesDir)
            Log.e("whh", "application.filesDir.path = " + application.filesDir.path)

            // 清除本应用内部缓存(/data/data/com.xxx.xxx/cache)
            if (application.cacheDir != null) {
                deleteFile(application.cacheDir.absolutePath)
            }
            // 清除外部cache下的内容(/mnt/sdcard/android/data/com.xxx.xxx/cache)
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED && application.externalCacheDir != null) {
                deleteFile(application.externalCacheDir!!.absolutePath)
            }
            // 清除本应用所有数据库(/data/data/com.xxx.xxx/databases)
            deleteFile("/data/data/"+ application.packageName + "/databases")
            // 清除本应用SharedPreference(/data/data/com.xxx.xxx/shared_prefs)
            deleteFile(("/data/data/" + application.packageName + "/shared_prefs"))
            // 清除/data/data/com.xxx.xxx/files下的内容
            if (application.filesDir != null) {
                deleteFile(application.filesDir.absolutePath)
            }
            for (filePath: String? in filepath) {
                cleanCustomCache(filePath)
            }
//            restartApp(activity);
        }

        /**
         * 重新启动 App
         */
        @JvmStatic
        fun restartApp(activity: Activity) {
            val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                activity.startActivity(intent)
            }
            //杀掉以前进程
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        /**
         * 清空公有目录（外部存储）
         * @return 内部存储清除成功返回 true，否则返回 false
         *              有存储文件清除失败时返回 false
         */
        @JvmStatic
        private fun clearExternalStorage(application: Application): Boolean {
            val externalPath = Environment.getExternalStorageDirectory()?.path + File.separator +
                                getPackageInfo(application)?.packageName
            Log.e("whh", "externalPath = $externalPath")
            val files = getFileDirectory(externalPath)
            if (null == files) {
                return false
            } else {
                var flag = true
                for (file in files) {
                    Log.e("whh", "clearExternalStorage - file?.getAbsolutePath() = " + file?.absolutePath)
                    if (file != null) {
                        flag = flag && deleteFile(file.absolutePath)
                    }
                }
                return flag
            }
        }

        /**
         * 清空私有目录（内部存储）
         * @return 内部存储清除成功返回 true，否则返回 false
         *          有存储文件清除失败时返回 false
         */
        @JvmStatic
        private fun clearInternalStorage(application: Application): Boolean {
            val files = getFileDirectory(application.filesDir?.parent)
            if (null == files) {
                return false
            } else {
                var flag = true
                for (file in files) {
                    Log.e("whh", "clearInternalStorage - file?.getAbsolutePath() = " + file?.absolutePath)
                    if (file != null && file.name != null && !file.name.contains("lib")) {
                        flag = flag && deleteFile(file.absolutePath)
                    }
                }
                return flag
            }
        }

        /**
         * 按名字清除本应用数据库
         */
        @JvmStatic
        fun cleanDatabaseByName(context: Context, dbName: String?) {
            context.deleteDatabase(dbName)
        }

        /**
         * 清除自定义路径下的文件，使用需小心，请不要误删。而且只支持目录下的文件删除
         * @param filePath 文件路径
         */
        @JvmStatic
        fun cleanCustomCache(filePath: String?) {
            deleteFile(filePath)
        }

        /**
         * 删除指定文件夹
         * @return 内部存储清除成功返回 true，否则返回 false
         *          有存储文件清除失败时返回 false
         */
        @JvmStatic
        private fun deleteDirectory(path: String?): Boolean {
            var filePath = path
            if (filePath != null && !filePath.endsWith(File.separator)) {
                filePath = filePath + File.separator
            }
            val files = getFileDirectory(filePath)
            if (null == files) {
                val dirFile = getFile(filePath)
                if (dirFile != null) {
                    return dirFile.delete()
                } else {
                    return false
                }
            } else {
                var flag = true
                for (file in files) {
                    if (file != null && file.isFile) {
                        flag = flag && deleteSingleFile(file.absolutePath)
//                        if (!flag) { // 文件删除失败，中止删除操作
//                            break
//                        }
                    } else if (file != null) {
                        flag = flag && deleteDirectory(file.absolutePath)
//                        if (!flag) { // 文件夹删除失败，中止删除操作
//                            break
//                        }
                    }
                }
                return flag
            }
        }

        /**
         * 删除单个文件
         *
         * @param filePath 被删除文件的文件路径
         * @return 文件删除成功返回true，否则返回false
         */
        @JvmStatic
        private fun deleteSingleFile(filePath: String?): Boolean {
            val file = getFile(filePath)
            if (file != null && file.isFile && file.exists()) {
                return file.delete()
            } else{
                return false
            }
        }

        /**
         * 根据路径删除指定的目录或文件，无论存在与否
         */
        @JvmStatic
        private fun deleteFile(filePath: String?): Boolean {
            val file = getFile(filePath)
            if (file == null || !file.exists()) {
                return false
            } else if (file.isFile) {
                return deleteSingleFile(filePath)
            } else if (file.isDirectory){
                return deleteDirectory(filePath)
            } else {
                return false
            }
        }


        /**
         * 获取路径对应的文件夹 中的 文件列表
         */
        @JvmStatic
        private fun getFileDirectory(path: String?): Array<File?>? {
            if (!TextUtils.isEmpty(path)) {
                val dir = File(path!!)
                if (dir != null && dir.exists() && dir.isDirectory) { // path 不为空时，file 一定不为空
                    if (dir.listFiles() != null) {
                        return dir.listFiles()
                    }
                }
            }
            return null
        }

        /**
         * 获取路径对应的文件
         */
        @JvmStatic
        private fun getFile(path: String?): File? {
            if (!TextUtils.isEmpty(path)) {
                return File(path!!)
            }
            return null
        }

        /**
         * 获取包信息
         */
        @JvmStatic
        private fun getPackageInfo(application: Application): PackageInfo? {
            val packageManager = application.packageManager
            var packInfo: PackageInfo? = null
            try {
                packInfo = packageManager.getPackageInfo(application.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            return packInfo
        }
    }
}