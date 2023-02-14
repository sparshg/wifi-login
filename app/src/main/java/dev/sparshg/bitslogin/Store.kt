package dev.sparshg.bitslogin

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.MultiProcessDataStoreFactory
import java.io.File

class Store(val context: Context) {

    // to make sure there is only one instance
    companion object {
        private var dataStore: DataStore<Settings>? = null
        fun getInstance(context: Context): DataStore<Settings> {
            dataStore ?: synchronized(this) {
                dataStore ?: Store(context).also {
                    dataStore = MultiProcessDataStoreFactory.create(
                        serializer = SettingsSerializer(),
                        produceFile = {
                            File("${context.filesDir.path}/settings")
                        }
                    )
                }
            }
            return dataStore!!
        }
    }

    suspend fun setCredSet(v: Boolean) {
        dataStore!!.updateData {
            it.copy(credSet = v)
        }
    }

    suspend fun setQsAdded(v: Boolean) {
        dataStore!!.updateData {
            it.copy(qsAdded = v)
        }
    }

    suspend fun setService(v: Boolean) {
        dataStore!!.updateData {
            it.copy(service = v)
        }
    }

    suspend fun setReview(v: Long) {
        dataStore!!.updateData {
            it.copy(review = v)
        }
    }

    suspend fun setAddress(v: Int) {
        dataStore!!.updateData {
            it.copy(address = v)
        }
    }

    suspend fun setUsername(v: String) {
        dataStore!!.updateData {
            it.copy(username = v)
        }
    }

    suspend fun setPassword(v: String) {
        dataStore!!.updateData {
            it.copy(password = v)
        }
    }
}