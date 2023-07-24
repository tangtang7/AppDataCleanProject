package com.example.appdatacleanproject;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

/**
 * 本应用数据清除管理器。主要功能有清除内/外缓存，清除数据库，清除sharedPreference，清除files和清除自定义目录
 */
public class DataCleanManager {
    /**
     *  方案一:利用命令行pm clear 包名，系统级别清除App数据
     *
     *  优点:和任务管理器里面清除所有数据的操作一致，会删除所有的APP数据。重新进入APP还需重新申请权限。
     *  缺点:系统会直接杀掉APP进程，无法进行拉起APP的操作。
     *  @param context
     *  @return
     */
    public static Process clearAppUserData(Context context) {
        Process p = execRuntimeProcess("pm clear " + context.getPackageName());
        Log.e("whh","context.getPackageName()) = " + context.getPackageName());
        return p;
    }

    public static Process execRuntimeProcess(String commond) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(commond);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return p;
    }

    /**
     *  方案二:手动删除内部储存和外部储存
     *
     *  优点:由自身控制，可以重新拉起APP。
     *  缺点:
     *      1.没有系统级的清除那么彻底，比如重新拉起后不需要重新申请权限。当然数据库，SharePreferences相关的通通都会删掉的
     *      2.因为数据库文件被删除，所以重新拉起启动页时需要删除当前进程，此时会黑屏一下。
     * 代码注意：说明一点下面代码中的ApplicationManager.getApplication()其实是获取了当前app的Application对象，也可以替换成context。
     */

    public static void use(Application application, Activity activity){
        //使用
        clearPublic(application);
        clearPrivate(application);
//        restartApp(activity);
    }

    public static void restartApp(Activity activity) {
        final Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
        }
        //杀掉以前进程
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * 清空公有目录
     */
    public static void clearPublic(Application application) {
        if (application == null) {
            throw new RuntimeException("App no init");
        }
        String publicFilePath = Environment.getExternalStorageDirectory().getPath() + "/" + getPackageInfo(application).packageName;
        Log.e("whh","publicFilePath = " + publicFilePath);
        File dir = new File(publicFilePath);
        File[] files = dir.listFiles();
        if (null != files) {
            for (File file : files) {
                deleteFolder(file.getAbsolutePath());
            }
        }
    }

    /**
     * 清空私有目录
     */
    public static  void clearPrivate(Application application) {
        if (application == null) {
            throw new RuntimeException("App no init");
        }
        //清空文件夹
        File dir = new File(application.getFilesDir().getParent());
        File[] files = dir.listFiles();
        if (null != files) {
            for (File file : files) {
                if (!file.getName().contains("lib")) {
                    deleteFolder(file.getAbsolutePath());
                    Log.e("whh","file.getAbsolutePath() = " + file.getAbsolutePath());
                }
            }
        }
    }

    /**
     * 删除指定文件
     */
    private static  boolean deleteDirectory(String filePath) {
        boolean flag = false;
        if (!filePath.endsWith(File.separator)) {
            filePath = filePath + File.separator;
        }
        File dirFile = new File(filePath);
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        flag = true;
        File[] files = dirFile.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                flag = deleteSingleFile(file.getAbsolutePath());
                if (!flag) {
                    break;
                }
            } else {
                flag = deleteDirectory(file.getAbsolutePath());
                if (!flag) {
                    break;
                }
            }
        }
        if (!flag) {
            return false;
        }
        return dirFile.delete();
    }

    /**
     * 删除单个文件
     *
     * @param filePath 被删除文件的文件名
     * @return 文件删除成功返回true，否则返回false
     */
    private static boolean deleteSingleFile(String filePath) {
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }
    /**
     * 删除方法 这里只会删除某个文件夹下的文件，如果传入的directory是个文件，将不做处理
     * @param directory
     */
    private static void deleteFilesByDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory() && directory.listFiles() != null) {
            for (File item : directory.listFiles()) {
                if (item != null) {
                    item.delete();
                }
            }
        }
    }
    /**
     * 根据路径删除指定的目录或文件，无论存在与否
     */
    private static boolean deleteFolder(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return false;
        } else {
            if (file.isFile()) {
                return deleteSingleFile(filePath);
            } else {
                return deleteDirectory(filePath);
            }
        }
    }

    /**
     * 获取包信息
     */
    private static PackageInfo getPackageInfo(Application application) {
        PackageManager packageManager = application.getPackageManager();
        PackageInfo packInfo = null;
        try {
            packInfo = packageManager.getPackageInfo(application.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return packInfo;
    }



    /**
     * 清除本应用所有的数据
     * @param context
     * @param filepath
     */
    public static void cleanApplicationData(Context context, String... filepath) {
        Log.e("whh","context.getCacheDir() = " + context.getCacheDir().getPath());
        Log.e("whh","context.getExternalCacheDir() = " + context.getExternalCacheDir().getPath());
        Log.e("whh","context.Databases = " + "/data/data/" + context.getPackageName() + "/databases");
        Log.e("whh","context.SharedPreference = " + "/data/data/" + context.getPackageName() + "/shared_prefs");
        Log.e("whh","context.getFilesDir() = " + context.getFilesDir());
        cleanInternalCache(context);
        cleanExternalCache(context);
        cleanDatabases(context);
        cleanSharedPreference(context);
        cleanFiles(context);
        for (String filePath : filepath) {
            cleanCustomCache(filePath);
        }
    }

    /**
     * 清除本应用内部缓存(/data/data/com.xxx.xxx/cache)
     * @param context
     */
    public static void cleanInternalCache(Context context) {
        deleteFilesByDirectory(context.getCacheDir());
    }

    /**
     * 清除外部cache下的内容(/mnt/sdcard/android/data/com.xxx.xxx/cache)
     * @param context
     */
    public static void cleanExternalCache(Context context) {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            deleteFilesByDirectory(context.getExternalCacheDir());
        }
    }

    /**
     * 清除本应用所有数据库(/data/data/com.xxx.xxx/databases)
     * @param context
     */
    public static void cleanDatabases(Context context) {
        deleteFilesByDirectory(new File("/data/data/"
                + context.getPackageName() + "/databases"));
    }

    /**
     * 按名字清除本应用数据库
     * @param context
     * @param dbName
     */
    public static void cleanDatabaseByName(Context context, String dbName) {
        context.deleteDatabase(dbName);
    }

    /**
     * 清除本应用SharedPreference(/data/data/com.xxx.xxx/shared_prefs)
     * @param context
     */
    public static void cleanSharedPreference(Context context) {
        deleteFilesByDirectory(new File("/data/data/"
                + context.getPackageName() + "/shared_prefs"));
    }

    /**
     * 清除/data/data/com.xxx.xxx/files下的内容
     * @param context
     */
    public static void cleanFiles(Context context) {
        deleteFilesByDirectory(context.getFilesDir());
    }

    /**
     * 清除自定义路径下的文件，使用需小心，请不要误删。而且只支持目录下的文件删除
     * @param filePath
     */
    public static void cleanCustomCache(String filePath) {
        deleteFilesByDirectory(new File(filePath));
    }


}
