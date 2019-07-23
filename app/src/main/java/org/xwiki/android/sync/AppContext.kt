/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.android.sync

import android.app.Application
import android.content.Context
import android.util.Log
import com.facebook.stetho.Stetho
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.xwiki.android.sync.contactdb.AppDatabase
import org.xwiki.android.sync.contactdb.AppRepository
import org.xwiki.android.sync.rest.BaseApiManager
import org.xwiki.android.sync.utils.getArrayList
import org.xwiki.android.sync.utils.putArrayList
import java.util.*

/**
 * Application class for authenticator
 *
 * @version $Id: c3a5996b1bce14d5c105a55f085115347c39c035 $
 */


/**
 * Entry pair Server address - Base Api Manager
 */
private lateinit var baseApiManager: Pair<String, BaseApiManager>

private var userCookie: String? = null

/**
 * Logging tag
 */
private const val TAG = "AppContext"

/**
 * Instance of context to use it in static methods
 * @return known AppContext instance
 */

lateinit var appContext: Context
    private set

val scope = CoroutineScope(Dispatchers.Default)

/**
 * @return actual base url
 */
fun getAccountServerUrl(accountName: String?): String {
    var serverUrl: String = ""
    if (accountName == null) {
        return XWIKI_DEFAULT_SERVER_ADDRESS
    } else {
        scope.launch {
            val userDao = AppDatabase.getInstance(appContext).userDao()
            val userRepository = AppRepository(userDao, null, null)
            val user = userRepository.findByAccountName(accountName)
            serverUrl = user.value?.serverAddress.toString()
            userCookie = user.value?.cookie
        }
    }
    return serverUrl
}

fun getUserCookie (accountName: String?): String {
    if (userCookie.isNullOrBlank()) {
        val userDao = AppDatabase.getInstance(appContext).userDao()
        val userRepository = AppRepository(userDao, null, null)
        val user = userRepository.findByAccountName(accountName.toString())
        userCookie = user.value?.cookie.toString()
    }
    return userCookie.toString()
}

fun getUserSyncType (accountName: String): Int {
    var syncType = 0
    scope.launch {
        val userDao = AppDatabase.getInstance(appContext).userDao()
        val userRepository = AppRepository(userDao, null, null)
        val user = userRepository.findByAccountName(accountName)
        user.value?.syncType?.let { it1 -> syncType = it1 }
    }
    return syncType
}

/**
 * Add app as authorized
 *
 * @param packageName Application package name to add as authorized
 */
fun addAuthorizedApp(packageName: String) {
    Log.d(TAG, "packageName=$packageName")
    var packageList: MutableList<String>? = getArrayList(appContext.applicationContext, PACKAGE_LIST)
    if (packageList == null) {
        packageList = ArrayList()
    }
    packageList.add(packageName)
    putArrayList(appContext.applicationContext, PACKAGE_LIST, packageList)
}

/**
 * Check that application with packageName is authorised.
 *
 * @param packageName Application package name
 * @return true if application was authorized
 */
fun isAuthorizedApp(packageName: String): Boolean {
    val packageList = getArrayList(
        appContext.applicationContext,
        PACKAGE_LIST
    )
    return packageList != null && packageList.contains(packageName)
}

/**
 * @return Current [.baseApiManager] value or create new and return
 *
 * @since 0.4
 */
val apiManager : BaseApiManager
    get() {
        val url = getAccountServerUrl(null)
        val manager = try {
            baseApiManager
        } catch (e: UninitializedPropertyAccessException) {
            baseApiManager = url to BaseApiManager(url)
            baseApiManager
        }
        return manager.second
    }

open class AppContext : Application() {

    /**
     * Set [.instance] to this object.
     */
    override fun onCreate() {
        super.onCreate()
        appContext = this
        Log.d(TAG, "on create")
        Stetho.initializeWithDefaults(this)
    }
}
