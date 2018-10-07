package com.billy.android.register

import com.android.builder.model.AndroidProject
import com.google.gson.Gson
import org.gradle.api.Project

import java.lang.reflect.Type

/**
 * 文件操作辅助类
 * @author zhangkb
 * @since 2018/04/13
 */
class RegisterCache {
    final static def CACHE_INFO_DIR = "cc-register"

    /**
     * 缓存自动注册配置的文件
     * @param project
     * @return file
     */
    static File getRegisterInfoCacheFile(Project project) {
        return getCacheFile(project, "register-info.json")
    }

    /**
     * 缓存扫描到结果的文件
     * @param project
     * @return File
     */
    static File getRegisterCacheFile(Project project) {
        return getCacheFile(project, "register-cache.json")
    }

    static File getBuildTypeCacheFile(Project project) {
        return getCacheFile(project, "build-type.json")
    }

    private static File getCacheFile(Project project, String fileName) {
        String baseDir = getCacheFileDir(project)
        if (mkdirs(baseDir)) {
            return new File(baseDir + fileName)
        } else {
            throw new FileNotFoundException("Not found  path:" + baseDir)
        }
    }

    static boolean isSameAsLastBuildType(Project project, boolean isApp) {
        File cacheFile = getCacheFile(project, "build-type.json")
        if (cacheFile.exists()) {
            return (cacheFile.text == 'true') == isApp
        }
        return false
    }

    static void cacheBuildType(Project project, boolean isApp) {
        File cacheFile = getCacheFile(project, "build-type.json")
        cacheFile.getParentFile().mkdirs()
        if (!cacheFile.exists())
            cacheFile.createNewFile()
        cacheFile.write(isApp.toString())
    }


    /**
     * 将扫描到的结果缓存起来
     * @param cacheFile
     * @param harvests
     */
    static void cacheRegisterHarvest(File cacheFile, String harvests) {
        if (!cacheFile || !harvests)
            return
        cacheFile.getParentFile().mkdirs()
        if (!cacheFile.exists())
            cacheFile.createNewFile()
        cacheFile.write(harvests)
    }

    private static String getCacheFileDir(Project project) {
        return project.getBuildDir().absolutePath + File.separator + AndroidProject.FD_INTERMEDIATES + File.separator + CACHE_INFO_DIR + File.separator
    }

    /**
     * 读取文件内容并创建Map
     * @param file 缓存文件
     * @param type map的类型
     * @return
     */
    static Map readToMap(File file, Type type) {
        Map map = null
        if (file.exists()) {
            if (type) {
                def text = file.text
                if (text) {
                    try {
                        map = new Gson().fromJson(text, type)
                    } catch (Exception e) {
                        e.printStackTrace()
                    }
                }
            }
        }
        if (map == null) {
            map = new HashMap()
        }
        return map
    }

    /**
     * 创建文件夹
     * @param dirPath
     * @return boolean
     */
    static boolean mkdirs(String dirPath) {
        def baseDirFile = new File(dirPath)
        def isSuccess = true
        if (!baseDirFile.isDirectory()) {
            isSuccess = baseDirFile.mkdirs()
        }
        return isSuccess
    }

}