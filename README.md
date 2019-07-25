# kotlin-flow-extensions
Extensions to the Kotlin Flow library.

<a href='https://travis-ci.org/akarnokd/kotlin-flow-extensions/builds'><img src='https://travis-ci.org/akarnokd/kotlin-flow-extensions.svg?branch=master'></a>
[![codecov.io](http://codecov.io/github/akarnokd/kotlin-flow-extensions/coverage.svg?branch=master)](http://codecov.io/github/akarnokd/kotlin-flow-extensions?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.akarnokd/kotlin-flow-extensions/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.akarnokd/kotlin-flow-extensions)

# Features

## PublishSubject

Multicasts values to one or more flow collectors in a coordinated fashion, awaiting all collectors to be ready
to receive the next item or termination.

```kotlin
import hu.akarnokd.kotlin.flow.*

runBlocking {
    
    val publishSubject = PublishSubject<Int>()

    val job = launch(Dispatchers.IO) {
        publishSubject.collect {
            println(it)
        }
        println("Done")
    }
   
    publishSubject.emit(1)
    publishSubject.complete()
   
    job.join()
}
```

## ReplaySubject

Caches and replays some or all items to collectors. Constructors for size-bound, time-bound and both size-and-time bound
replays are available. An additional constructor with a `TimeUnit -> Long` has been defined to allow virtualizing
the progression of time for testing purposes

```kotlin
import hu.akarnokd.kotlin.flow.*

runBlocking {
    
    val replaySubject = ReplaySubject<Int>()

    val job = launch(Dispatchers.IO) {
        replaySubject.collect {
            println(it)
        }
        println("Done")
    }
   
    replaySubject.emit(1)
    replaySubject.emit(2)
    replaySubject.emit(3)
    replaySubject.complete()
   
    job.join()

    replaySubject.collect {
        println(it)
    }
    println("Done 2")
}
```