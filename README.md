# Android-Jatpack-DataStore-Sample
# 简介
`Jetpack DataStore` 是一种数据存储解决方案，允许您使用协议缓冲区存储键值对或类型化对象。DataStore 使用 Kotlin 协程和流程以异步、一致的事务方式存储数据。

如果您当前在使用 SharedPreferences 存储数据，请考虑迁移到 DataStore。

> 注意：如果您需要支持大型或复杂数据集、部分更新或参照完整性，请考虑使用 Room，而不是 DataStore。DataStore非常适合简单的小型数据集，不支持部分更新或参照完整性。

`Preferences DataStore` 和 `Proto DataStore`
DataStore 提供两种不同的实现：Preferences DataStore 和 Proto DataStore。

`Preferences DataStore` 使用键存储和访问数据。此实现不需要预定义的架构，也不确保类型安全。
`Proto DataStore` 将数据作为自定义数据类型的实例进行存储。此实现要求您使用协议缓冲区来定义架构，但可以确保类型安全。

# 设置
如需在您的应用中使用 Jetpack DataStore，请根据您要使用的实现向 Gradle 文件添加以下内容。
如果是使用`Protobuf DataStore`

```groovy
// Typed DataStore (Typed API surface, such as Proto)
dependencies {
  implementation "androidx.datastore:datastore:1.0.0-alpha06"

  // optional - RxJava2 support
  implementation "androidx.datastore:datastore-rxjava2:1.0.0-alpha06"

  // optional - RxJava3 support
  implementation "androidx.datastore:datastore-rxjava3:1.0.0-alpha06"
}
// Alternatively - use the following artifact without an Android dependency.
dependencies {
  implementation "androidx.datastore:datastore-core:1.0.0-alpha06"
}
```
如果是使用`Preferences DataStore`

```groovy
// Preferences DataStore (SharedPreferences like APIs)
dependencies {
  implementation "androidx.datastore:datastore-preferences:1.0.0-alpha06"

  // optional - RxJava2 support
  implementation "androidx.datastore:datastore-preferences-rxjava2:1.0.0-alpha06"

  // optional - RxJava3 support
  implementation "androidx.datastore:datastore-preferences-rxjava3:1.0.0-alpha06"
}
// Alternatively - use the following artifact without an Android dependency.
dependencies {
  implementation "androidx.datastore:datastore-preferences-core:1.0.0-alpha06"
}
```
# 使用 Preferences DataStore 存储键值对
Preferences DataStore 实现使用 DataStore 和 Preferences 类将简单的键值对保留在磁盘上。

 - **创建 Preferences DataStore**

使用 `Context.createDataStore()` 扩展函数创建 `DataStore<Preferences>` 的实例。此外，如果您使用的是 `RxJava`，请使用 `RxPreferenceDataStoreBuilder`。必需的 name 参数是 Preferences DataStore 的名称。

```kotlin
kotlin代码
val dataStore: DataStore<Preferences> = context.createDataStore(
  name = "settings"
)
```
或者
```java
Java代码
DataStore<Preferences> dataStore =
  new RxPreferenceDataStoreBuilder(context, /*name=*/ "settings").build();
```

 - **从 Preferences DataStore 读取内容**

由于 Preferences DataStore 不使用预定义的架构，因此您必须使用相应的键类型函数为需要存储在 `DataStore<Preferences>` 实例中的每个值定义一个键。例如，如需为 int 值定义一个键，请使用 `intPreferencesKey()`。然后，使用 DataStore.data 属性，通过 Flow 提供适当的存储值。

```kotlin
kotlin代码
val EXAMPLE_COUNTER = intPreferencesKey("example_counter")
val exampleCounterFlow: Flow<Int> = dataStore.data
  .map { preferences ->
    // No type safety.
    preferences[EXAMPLE_COUNTER] ?: 0
}
```
或者
```java
Java代码
Preferences.Key<Integer> EXAMPLE_COUNTER = PreferencesKeys.int("example_counter");

Flowable<Integer> exampleCounterFlow =
  RxDataStore.data(dataStore).map(prefs -> prefs.get(EXAMPLE_COUNTER));
```

 - **将内容写入 Preferences DataStore**

Preferences DataStore 提供了一个 `edit()` 函数，用于以事务方式更新 DataStore 中的数据。该函数的 transform 参数接受代码块，您可以在其中根据需要更新值。转换块中的所有代码均被视为单个事务。

```kotlin
kotlin代码
suspend fun incrementCounter() {
  dataStore.edit { settings ->
    val currentCounterValue = settings[EXAMPLE_COUNTER] ?: 0
    settings[EXAMPLE_COUNTER] = currentCounterValue + 1
  }
}
```
或者
```java
Java代码
Single<Preferences> updateResult =  RxDataStore.updateDataAsync(dataStore, prefsIn -> {
  MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
  Integer currentInt = prefsIn.get(INTEGER_KEY);
  mutablePreferences.set(INTEGER_KEY, currentInt != null ? currentInt + 1 : 1);
  return Single.just(mutablePreferences);
});
// The update is completed once updateResult is completed.
```
# 从 SharedPreference 迁移数据
`Google` 推出 DataStore 的目的是为了取代 `SharedPreference`，对于老项目，就需要从 SharedPreference 中进行数据的迁移，`从 SharedPreference 迁移到 DataStore`。

在  `createDataStore` 方法的参数中有 `migrations` 参数：

```kotlin
migrations: List<DataMigration<Preferences>> = listOf()
```

只需要在 createDataStore 方法中按照如下格式就可以自动完成数据的迁移：

```kotlin
   mDataStorePre = this.createDataStore(
            name = DATASTORE_PREFERENCE_NAME,
            migrations = listOf(SharedPreferencesMigration(this, SP_PREFERENCE_NAME))
        )
```

# 使用 Proto DataStore 存储类型化的对象
`Proto DataStore` 实现使用 DataStore 和协议缓冲区将类型化的对象保留在磁盘上。
关于Protobuf的使用，请参考[protobuf官方文档](https://developers.google.com/protocol-buffers)

 - **Proto DataStore 的创建**

在创建 Proto DataStore 的时候，在 AndroidStudio 中，必须先做如下配置：

在 project 的 build.gradle 中添加依赖：

```groovy
classpath 'com.google.protobuf:protobuf-gradle-plugin:0.8.15'
```
在 app 的 build.gradle 中：

```groovy
plugins {
    id 'com.android.application'
}
apply plugin: 'kotlin-android'
apply plugin: 'com.google.protobuf'

//https://github.com/google/protobuf-gradle-plugin
android {
    compileSdkVersion 29
    buildToolsVersion "30.0.2"

   defaultConfig {
        applicationId "com.ryd.datastore"
        minSdkVersion 16
        targetSdkVersion 29
        versionCode 1
        versionName "1.0"

        // 开起分包
        multiDexEnabled true

    }

   buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

   sourceSets {
        main {
            proto {
                //The plugin adds a new sources block named proto alongside java to every sourceSet. By default, it includes all *.proto files under src/$sourceSetName/proto. You can customize it in the same way as you would customize the java sources.
                srcDir 'src/main/proto'
                include '**/*.proto'
            }
        }
    }


}

dependencies {

   implementation 'androidx.appcompat:appcompat:1.2.0'
    implementation 'com.google.android.material:material:1.3.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.0.4'

   //======================
    //  Typed DataStore (Typed API surface, such as Proto)
    implementation "androidx.datastore:datastore:1.0.0-alpha06"

   // optional - RxJava2 support
    implementation "androidx.datastore:datastore-rxjava2:1.0.0-alpha06"

   // optional - RxJava3 support
    //implementation "androidx.datastore:datastore-rxjava3:1.0.0-alpha06"

   //=======================
    // Preferences DataStore (SharedPreferences like APIs)
    implementation "androidx.datastore:datastore-preferences:1.0.0-alpha06"

   // optional - RxJava2 support
    implementation "androidx.datastore:datastore-preferences-rxjava2:1.0.0-alpha06"

   // optional - RxJava3 support
    //implementation "androidx.datastore:datastore-preferences-rxjava3:1.0.0-alpha06"

   //==================
    // kotlin
    implementation "androidx.core:core-ktx:1.3.2"
    implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version"

   // multidex
    implementation 'com.android.support:multidex:1.0.3'

    // protobuf
    implementation 'com.google.protobuf:protobuf-javalite:3.10.0'


}

protobuf {
      // Configure the protoc executable
    protoc {
        // Download from repositories
        artifact = "com.google.protobuf:protoc:3.10.0"
    }

   // Generates the java Protobuf-lite code for the Protobufs in this project. See
    // https://github.com/google/protobuf-gradle-plugin#customizing-protobuf-compilation
    // for more information.
    generateProtoTasks {
        // all() returns the collection of all protoc tasks
        all().each { task ->
            //配置要生成的内容
            //代码生成是由protoc内置和插件完成的。每个内置/插件都会生成特定类型的代码。要在任务上添加或配置内置/插件，请列出其名称，后跟大括号块。如果需要的话，在括号里加上选项。例如：
            task.builtins {
                java {
                    option 'lite'
                }
            }
        }
    }
}


repositories {
    mavenCentral()
}
```

 - **定义架构**

Proto DataStore 要求在 app/src/main/proto/ 目录的 proto 文件中保存预定义的架构。此架构用于定义您在 Proto DataStore 中保存的对象的类型。如需详细了解如何定义 proto 架构，请参阅 [protobuf官方文档](https://developers.google.com/protocol-buffers)。

```java
syntax = "proto3";

option java_package = "com.example.application";
option java_multiple_files = true;

message Settings {
  int32 example_counter = 1;
}
```

> 注意：您的存储对象的类在编译时由 proto 文件中定义的 message 生成。请务必重新构建您的项目。

记得要执行 rebuild project 

 - **创建 Proto DataStore**

创建 `Proto DataStore` 来存储类型化对象涉及两个步骤：

 1. 定义一个实现 Serializer<T> 的类，其中 T 是 proto 文件中定义的类型。此序列化器类会告知 DataStore
    如何读取和写入您的数据类型。请务必为该序列化器添加默认值，以便在尚未创建任何文件时使用。
 2. 使用 `Context.createDataStore()` 扩展函数创建 DataStore<T> 的实例，其中 T 是在 proto
    文件中定义的类型。filename 参数会告知 DataStore 使用哪个文件存储数据，而 serializer 参数会告知DataStore 第 1 步中定义的序列化器类的名称。
```kotlin
    kotlin代码
object SettingsSerializer : Serializer<Settings> {
  override val defaultValue: Settings = Settings.getDefaultInstance()

  override fun readFrom(input: InputStream): Settings {
    try {
      return Settings.parseFrom(input)
    } catch (exception: InvalidProtocolBufferException) {
      throw CorruptionException("Cannot read proto.", exception)
    }
  }

  override fun writeTo(
    t: Settings,
    output: OutputStream) = t.writeTo(output)
}

val settingsDataStore: DataStore<Settings> = context.createDataStore(
  fileName = "settings.pb",
  serializer = SettingsSerializer
)
```
或者

```java
Java代码
private static class SettingsSerializer implements Serializer<Settings> {
  @Override
  public Settings getDefaultValue() {
    Settings.getDefaultInstance();
  }

  @Override
  public Settings readFrom(@NotNull InputStream input) {
    try {
      return Settings.parseFrom(input);
    } catch (exception: InvalidProtocolBufferException) {
      throw CorruptionException(“Cannot read proto.”, exception);
    }
  }

  @Override
  public void writeTo(Settings t, @NotNull OutputStream output) {
    t.writeTo(output);
  }
}

DataStore<Settings> dataStore =
    new RxDataStoreBuilder<Settings>(context, /* fileName= */ "settings.pb", new SettingsSerializer()).build();
```

 - **从 Proto DataStore 读取内容**

使用 DataStore.data 显示所存储对象中相应属性的 Flow。

```kotlin
kotlin代码
val exampleCounterFlow: Flow<Int> = settingsDataStore.data
  .map { settings ->
    // The exampleCounter property is generated from the proto schema.
    settings.exampleCounter
  }
```
或者

```java
Java代码
Flowable<Integer> exampleCounterFlow =
  RxDataStore.data(dataStore).map(settings -> settings.getExampleCounter());
```

 - **将内容写入 Proto DataStore**

Proto DataStore 提供了一个 `updateData()` 函数，用于以事务方式更新存储的对象。updateData() 为您提供数据的当前状态，作为数据类型的一个实例，并在原子读-写-修改操作中以事务方式更新数据。

```kotlin
kotlin代码
suspend fun incrementCounter() {
  settingsDataStore.updateData { currentSettings ->
    currentSettings.toBuilder()
      .setExampleCounter(currentSettings.exampleCounter + 1)
      .build()
    }
}
```
或者

```java
Java代码
Single<Settings> updateResult =
  RxDataStore.updateDataAsync(dataStore, currentSettings ->
    Single.just(
      currentSettings.toBuilder()
        .setExampleCounter(currentSettings.getExampleCounter() + 1)
        .build()));
```
# 在同步代码中使用 DataStore

> 注意：请尽可能避免在 DataStore 数据读取时阻塞线程。阻塞界面线程可能会导致 ANR 或界面卡顿，而阻塞其他线程可能会导致死锁。

DataStore 的主要优势之一是异步 API，但可能不一定始终能将周围的代码更改为异步代码。如果您使用了采用同步磁盘 I/O 的现有代码库，或者您的依赖项不提供异步 API，就可能出现这种情况。

`Kotlin` 协程提供 `runBlocking()` 协程构建器，以帮助消除同步与异步代码之间的差异。您可以使用 runBlocking() 从 DataStore 同步读取数据。RxJava 在 Flowable 上提供阻塞方法。以下代码会阻塞发起调用的线程，直到 DataStore 返回数据：

```kotlin
kotlin代码
val exampleData = runBlocking { dataStore.data.first() }
```
或者
```java
Java代码
Settings settings = RxDataStore.data(dataStore).blockingFirst();
```
对界面线程执行同步 I/O 操作可能会导致 ANR 或界面卡顿。您可以通过从 DataStore 异步预加载数据来减少这些问题：

```kotlin
kotlin代码
override fun onCreate(savedInstanceState: Bundle?) {
    lifecycleScope.launch {
        dataStore.data.first()
        // You should also handle IOExceptions here.
    }
}
```
或者

```java
Java代码
RxDataStore.data(dataStore).first().subscribe();
```
这样，DataStore 可以异步读取数据并将其缓存在内存中。以后使用 runBlocking() 进行同步读取的速度可能会更快，或者如果初始读取已经完成，可能也可以完全避免磁盘 I/O 操作。

[DataStore的Demo传送门](https://github.com/ruanyandong/Android-Jatpack-DataStore-Sample)

参考文档
[Jetpack DataStore官方文档](https://developer.android.com/topic/libraries/architecture/datastore#kotlin)

[Android Jetpack 之 DataStore博客](https://blog.csdn.net/zzw0221/article/details/109274610)

[google/protobuf-gradle-plugin](https://github.com/google/protobuf-gradle-plugin)

[protobuf官方文档](https://developers.google.com/protocol-buffers)







