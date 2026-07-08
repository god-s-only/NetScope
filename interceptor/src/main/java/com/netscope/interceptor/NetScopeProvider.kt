package com.netscope.interceptor

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri


class NetScopeProvider : ContentProvider() {
    override fun onCreate(): Boolean = true
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun query(uri: Uri, p: Array<String>?, s: String?,
                       sA: Array<String>?, sO: String?): Cursor? = null
    override fun update(uri: Uri, v: ContentValues?,
                        s: String?, sA: Array<String>?): Int = 0
    override fun delete(uri: Uri, s: String?, sA: Array<String>?): Int = 0
    override fun getType(uri: Uri): String? = null
}