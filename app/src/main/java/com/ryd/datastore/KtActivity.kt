package com.ryd.datastore

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.createDataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.createDataStore
import androidx.datastore.preferences.protobuf.InvalidProtocolBufferException
import com.ryd.datastore.proto.Settings
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

// Android Studio引入protobufs
// https://blog.csdn.net/zzw0221/article/details/109274610
// https://developer.android.com/codelabs/android-proto-datastore#4
////https://github.com/google/protobuf-gradle-plugin

public class KtActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kt)

        // 使用 Preferences DataStore 存储键值对
        //=================================================================
        // 第一步 创建 Preferences DataStore
        val dataStore: DataStore<Preferences> = this.createDataStore(name = "pref_kt")

        // 定义键
        val COUNTER_KEY: Preferences.Key<Int> = intPreferencesKey("counter_kt_key")

        // 协程作用域
        GlobalScope.launch {
            // 第二步，定义键，读取值
            readValue(dataStore, COUNTER_KEY)

            delay(1000)

            // 第三步，写入值
            writeCounter(dataStore, COUNTER_KEY)
        }
        //==================================================================================

        // ===============使用 Proto DataStore 存储类型化的对象================
        // 第一步 创建 Proto DataStore
        val protoDataStore: DataStore<Settings> = this.createDataStore(fileName = "kt_proto_settings.pb", serializer = SettingsSerializer)

        GlobalScope.launch {
            // 第二步 读取值
            readProtoValue(protoDataStore)
            // 第三步 写入值
            writeProtoValue(protoDataStore)
            // 第四步 读取值验证
            readProtoValue(protoDataStore)
        }


    }


    // =========preferences方式==============================================
    /**
     * 第二步 从Preferences DataStore 读取内容
     */
    private suspend fun readValue(dataStore: DataStore<Preferences>, COUNTER_KEY: Preferences.Key<Int>) {
        // 第二步 从Preferences DataStore 读取内容
        val counterFlow: Flow<Int> = dataStore.data.map(transform = { value: Preferences ->
            val counter = value[COUNTER_KEY]
            Log.d("ruanyandong", "readValue: map counter $counter")
            return@map counter ?: 0
        })
        val counter: Int = counterFlow.first()

        Log.d("ruanyandong", "readValue: counter $counter")
    }

    /**
     * 第三步 将内容写入 Preferences DataStore
     */
    private suspend fun writeCounter(dataStore: DataStore<Preferences>, COUNTER_KEY: Preferences.Key<Int>) {
        dataStore.edit { settings ->
            Log.d("ruanyandong", "writeCounter: counter ${settings[COUNTER_KEY]}")
            val currentCounterValue = settings[COUNTER_KEY] ?: 0
            settings[COUNTER_KEY] = currentCounterValue + 1
            Log.d("ruanyandong", "writeCounter: settings[COUNTER_KEY] ${settings[COUNTER_KEY]}")
            settings[COUNTER_KEY]
        }
    }

    //=============protobufs方式============================================================

    /**
     * 定义一个实现 Serializer<T> 的类，其中 T 是 proto 文件中定义的类型。此序列化器类会告知 DataStore 如何读取和写入您的数据类型。请务必为该序列化器添加默认值，以便在尚未创建任何文件时使用。
     */
    object SettingsSerializer : Serializer<Settings> {

        override val defaultValue: Settings = Settings.getDefaultInstance()

        override fun readFrom(input: InputStream): Settings {
            try {
                return Settings.parseFrom(input)
            } catch (exception: InvalidProtocolBufferException) {
                throw CorruptionException("Cannot read proto.", exception)
            }
        }

        override fun writeTo(t: Settings, output: OutputStream) {
            t.writeTo(output)
        }

    }

    /**
     *  从proto中读取值
     */
    private suspend fun readProtoValue(protoDataStore: DataStore<Settings>) {
        val protoCounterFlow: Flow<Int> = protoDataStore.data.map(transform = { value: Settings ->
            val int: Int = value.kotlinCounter
            Log.d("ruanyandong", "readProtoValue: proto map int $int")
            return@map int
        })
        val counter: Int = protoCounterFlow.first()
        Log.d("ruanyandong", "readProtoValue: proto counter $counter")
    }

    /**
     * 向proto写入值
     */
    private suspend fun writeProtoValue(protoDataStore: DataStore<Settings>) {
        protoDataStore.updateData(transform = { currentSettings ->
            return@updateData currentSettings.toBuilder().setKotlinCounter(currentSettings.kotlinCounter + 1).build()
        })
    }


}
