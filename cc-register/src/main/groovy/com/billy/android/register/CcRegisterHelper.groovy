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
class CcRegisterHelper {
    final static def CACHE_INFO_DIR = "cc-register"

    /**
     * 缓存自动注册配置的文件
     * @param project
     * @return file
     */
    static File getRegisterInfoCacheFile(Project project) {
        String baseDir = getCacheFileDir(project)
        if (mkdirs(baseDir)) {
            return new File(baseDir + "register-info.config")
        } else {
            throw new FileNotFoundException("Not found  path:" + baseDir)
        }
    }

    /**
     * 缓存扫描到结果的文件
     * @param project
     * @return File
     */
    static File getRegisterCacheFile(Project project) {
        String baseDir = getCacheFileDir(project)
        if (mkdirs(baseDir)) {
            return new File(baseDir + "register-cache.json")
        } else {
            throw new FileNotFoundException("Not found  path:" + baseDir)
        }
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