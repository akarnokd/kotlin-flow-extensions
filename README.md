# kotlin-flow-extensions
Extensions to the Kotlin Flow library.

<a href='https://travis-ci.org/akarnokd/kotlin-flow-extensions/builds'><img src='https://travis-ci.org/akarnokd/kotlin-flow-extensions.svg?branch=master'></a>
[![codecov.io](http://codecov.io/github/akarnokd/kotlin-flow-extensions/coverage.svg?branch=master)](http://codecov.io/github/akarnokd/kotlin-flow-extensions?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.akarnokd/kotlin-flow-extensions/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.akarnokd/kotlin-flow-extensions)

## dependency

[Maven](https://search.maven.org/search?q=com.github.akarnokd)

```groovy
dependencies {
    implementation "com.github.akarnokd:kotlin-flow-extensions:0.0.4"
}
```

# Features

Table of contents

- Hot Flows
  - [PublishSubject](#publishsubject)
  - [ReplaySubject](#replaysubject)
  - [BehaviorSubject](#behaviorsubject)
- Sources
  - `range`
  - `timer`
- Intermediate Flow operators (`FlowExtensions`)
  - `Flow.concatWith`
  - `Flow.groupBy`
  - `Flow.parallel`
  - `Flow.publish`
  - `Flow.replay`
  - `Flow.startCollectOn`
  - `Flow.takeUntil`
  - `Flow.onBackpressureDrop`
  - [`Flow.flatMapDrop`](#flowflatmapdrop)
- `ParallelFlow` operators (`FlowExtensions`)
  - `ParallelFlow.concatMap`
  - `ParallelFlow.filter`
  - `ParallelFlow.map`
  - `ParallelFlow.reduce`
  - `ParallelFlow.sequential`
  - `ParallelFlow.transform`
- `ConnectableFlow`
  
## PublishSubject

Multicasts values to one or more flow collectors in a coordinated fashion, awaiting each collector to be ready
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
    
    // wait for the collector to arrive
    while (!publishSubject.hasCollectors()) {
        delay(1)
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
   
    // wait for the collector to arrive
    while (!replaySubject.hasCollectors()) {
        delay(1)
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

## BehaviorSubject

Caches the last item received and multicasts it and subsequent items (continuously) to collectors, awaiting each collector to be ready
to receive the next item or termination. It is possible to set an initial value to be sent to fresh collectors via a constructor.

```kotlin
import hu.akarnokd.kotlin.flow.*

runBlocking {
    
    val behaviorSubject = BehaviorSubject<Int>()
    behaviorSubject.emit(1)
  
    // OR
    // val behaviorSubject = BehaviorSubject<Int>(1)


    val job = launch(Dispatchers.IO) {
        behaviorSubject.collect {
            println(it)
        }
        println("Done")
    }
   
    // wait for the collector to arrive
    while (!behaviorSubject.hasCollectors()) {
        delay(1)
    }

    behaviorSubject.emit(2)
    behaviorSubject.emit(3)
    behaviorSubject.complete()
   
    job.join()
}
```

## Flow.flatMapDrop

Maps the upstream value into a `Flow` and relays its items while ignoring further upstream items until the current
inner `Flow` completes.

```kotlin
import hu.akarnokd.kotlin.flow.*

range(1, 10)
.map {
    delay(100)
    it
}
.flatMapDrop {
    range(it * 100, 5)
            .map {
                delay(30)
                it
            }
}
.assertResult(
        100, 101, 102, 103, 104,
        300, 301, 302, 303, 304,
        500, 501, 502, 503, 504,
        700, 701, 702, 703, 704,
        900, 901, 902, 903, 904
)
```

