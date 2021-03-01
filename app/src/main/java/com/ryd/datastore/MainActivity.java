package com.ryd.datastore;

import androidx.appcompat.app.AppCompatActivity;
import androidx.datastore.core.DataStore;
import androidx.datastore.core.Serializer;
import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava2.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava2.RxDataStore;
import androidx.datastore.rxjava2.RxDataStoreBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import com.ryd.datastore.proto.Settings;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.tv).setOnClickListener(v -> startActivity(new Intent(MainActivity.this, KtActivity.class)));

        // Preference方式使用DataStore
//=============================================================
        // 第一步 创建 Preferences DataStore
        DataStore<Preferences> dataStore = new RxPreferenceDataStoreBuilder(this,/*name*/"pref_settings").build();

        // 第二步 从Preferences DataStore 读取内容
        // 定义键
        Preferences.Key<Integer> COUNTER_KEY = PreferencesKeys.intKey("counter_key");
        // 读取值
        readValue(dataStore, COUNTER_KEY);

        // 第三步 将内容写入 Preferences DataStore , 注意这里是异步执行的，会有时序问题，所以实际使用时要注意
        new Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        // 写值
                        writeValue(dataStore, COUNTER_KEY);
                    }
                }, 2000);
//=================================================================

        //使用 Proto DataStore 存储类型化的对象

        // 创建DataStore对象
        DataStore<Settings> protoDataStore = new RxDataStoreBuilder<Settings>(MainActivity.this, /* fileName= */ "proto_settings.pb", new SettingsSerializer()).build();

        // 读取proto的值
        readValue(protoDataStore);

        // 写入proto值
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                writeValue(protoDataStore);
            }
        }, 2000);


    }

    //================preferences=============================

    /**
     * 将值写入Preference
     * @param dataStore
     * @param COUNTER_KEY
     */
    private void writeValue(DataStore<Preferences> dataStore, Preferences.Key<Integer> COUNTER_KEY) {
        Single<Preferences> updateResult = RxDataStore.updateDataAsync(dataStore, prefsInt -> {
            MutablePreferences mutablePreferences = prefsInt.toMutablePreferences();
            Integer currentInt = prefsInt.get(COUNTER_KEY);
            Log.d("ruanyandong", "writeValue: currentInt " + currentInt);
            mutablePreferences.set(COUNTER_KEY, currentInt != null ? currentInt + 1 : 1);
            return Single.just(mutablePreferences);

        });
        updateResult.subscribe(new Consumer<Preferences>() {
            @Override
            public void accept(Preferences preferences) throws Exception {
                if (preferences != null) {
                    Integer Int = preferences.get(COUNTER_KEY);
                    Log.d("ruanyandong", "accept: " + Int);
                }
            }
        });
    }

    boolean isComplete = false;

    /**
     * 读取Preference里的值
     * @param dataStore
     * @param COUNTER_KEY
     */
    private void readValue(DataStore<Preferences> dataStore, Preferences.Key<Integer> COUNTER_KEY) {
        // 获取值
        Disposable disposable = RxDataStore.data(dataStore).map(preferences -> {
            Integer integer = preferences.get(COUNTER_KEY);
            Log.d("ruanyandong", "read: 读取 " + integer);
            return integer == null ? 0 : integer;
        }).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.d("ruanyandong", "accept: 读取 " + integer);
                isComplete = true;
            }
        });// 需要订阅才能获取到值

        while (true) {
            if (isComplete) {
                disposable.dispose();// 取消订阅
                isComplete = false;
                break;
            }
        }
    }


    //===========proto======================================

    /**
     * 定义一个实现 Serializer<T> 的类，其中 T 是 proto 文件中定义的类型。此序列化器类会告知 DataStore 如何读取和写入您的数据类型。请务必为该序列化器添加默认值，以便在尚未创建任何文件时使用。
     */
    private static class SettingsSerializer implements Serializer<Settings> {

        @Override
        public Settings getDefaultValue() {
            return Settings.getDefaultInstance();
        }

        @Override
        public Settings readFrom(@NotNull InputStream inputStream) {
            try {
                return Settings.parseFrom(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public void writeTo(Settings settings, @NotNull OutputStream outputStream) {
            try {
                settings.writeTo(outputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    boolean isEnd = false;

    /**
     * 读取proto的值
     * @param protoDataStore
     */
    private void readValue(DataStore<Settings> protoDataStore) {
        Disposable disposable = RxDataStore.data(protoDataStore).map(new Function<Settings, Integer>() {
            @Override
            public Integer apply(@NonNull Settings settings) throws Exception {
                Integer integer = settings.getExampleCounter();
                Log.d("ruanyandong", "apply: proto 读取 " + integer);
                return integer;
            }
        }).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.d("ruanyandong", "accept: proto 读取 " + integer);
                isEnd = true;
            }
        });
        while (true) {
            if (isEnd) {
                disposable.dispose();
                isEnd = false;
                break;
            }
        }
    }

    /**
     * 值写入proto
     * @param protoDataStore
     */
    private void writeValue(DataStore<Settings> protoDataStore) {
        Single<Settings> updateResult = RxDataStore.updateDataAsync(protoDataStore, currentSettings -> Single.just(currentSettings.toBuilder().setExampleCounter(currentSettings.getExampleCounter() + 1).build()));
        updateResult.subscribe(new Consumer<Settings>() {
            @Override
            public void accept(Settings settings) throws Exception {
                if (settings != null) {
                    Integer integer = settings.getExampleCounter();
                    Log.d("ruanyandong", "accept: proto 写入后 " + integer);
                }
            }
        });
    }


}