# kotlin-flow-extensions
Extensions to the Kotlin Flow library.

<a href='https://github.com/akarnokd/kotlin-flow-extensions/actions?query=workflow%3A%22Java+CI+with+Gradle%22'><img src='https://github.com/akarnokd/kotlin-flow-extensions/workflows/Java%20CI%20with%20Gradle/badge.svg'></a>
[![codecov.io](http://codecov.io/github/akarnokd/kotlin-flow-extensions/coverage.svg?branch=master)](http://codecov.io/github/akarnokd/kotlin-flow-extensions?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.akarnokd/kotlin-flow-extensions/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.akarnokd/kotlin-flow-extensions)

## dependency

[Maven](https://search.maven.org/search?q=com.github.akarnokd)

```groovy
dependencies {
    implementation "com.github.akarnokd:kotlin-flow-extensions:0.0.14"
}
```

# Features

Table of contents

- Hot Flows
  - [PublishSubject](#publishsubject)
  - [ReplaySubject](#replaysubject)
  - [BehaviorSubject](#behaviorsubject)
  - [UnicastSubject](#unicastsubject)
  - [UnicastWorkSubject](#unicastworksubject)
- Sources
  - `range`
  - `timer`
  - [`concatArrayEager`](#concatarrayeager)
- Intermediate Flow operators (`FlowExtensions`)
  - `Flow.concatWith`
  - `Flow.groupBy`
  - `Flow.parallel`
  - [`Flow.publish`](#flowpublish)
  - `Flow.replay`
  - `Flow.startCollectOn`
  - `Flow.takeUntil`
  - `Flow.onBackpressureDrop`
  - [`Flow.flatMapDrop`](#flowflatmapdrop)
  - [`Flow.concatMapEager`](#flowconcatmapeager)
  - [`Flow.amb`](#flowamb)
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

## Flow.publish

Shares a single connection to the upstream source which can be consumed by many collectors inside a `transform` function,
which then yields the resulting items for the downstream.

Effectively, one collector to the output `Flow<R>` will trigger exactly one collection of the upstream `Flow<T>`. Inside
the `transformer` function though, the presented `Flow<T>` can be collected as many times as needed; it won't trigger
new collections towards the upstream but share items to all inner collectors as they become available.

Unfortunately, the suspending nature of coroutines/`Flow` doesn't give a clear indication when the `transformer` chain
has been properly established, which can result in item loss or run-to-completion without any item being collected.
If the number of the inner collectors inside `transformer` can be known, the `publish(expectedCollectors)` overload
can be used to hold back the upstream until the expected number of collectors have started/ready collecting items.

#### Example:

```kotlin
    range(1, 5)
    .publish(2) { 
         shared -> merge(shared.filter { it % 2 == 0 }, shared.filter { it % 2 != 0 }) 
    }
    .assertResult(1, 2, 3, 4, 5)
```

In the example, it is known `merge` will establish 2 collectors, thus the `publish` can be instructed to await those 2.
Without the argument, `range` would rush through its items as `merge` doesn't start collecting in time, causing an
empty result list.

## UnicastSubject

Buffers items until a single collector starts collecting items. Use `collectorCancelled` to
detect when the collector no longer wants to collect items.

Note that the subject uses an unbounded inner buffer and does not suspend its input side if
the collector never arrives or can't keep up.

```kotlin
val us = UnicastSubject()

launchIn(Dispatchers.IO) {
    for (i in 1..200) {
        println("Emitting $i")
        us.emit(i)
        delay(1)
    }
    emit.complete()
}

// collector arrives late for some reason
delay(100)

us.collect { println("Collecting $it") }
```

## UnicastWorkSubject

Buffers items until and inbetween a single collector is able to collect items. If the current
collector cancels, the next collector will receive the subsequent items.

Note that the subject uses an unbounded inner buffer and does not suspend its input side if
the collector never arrives or can't keep up.

```kotlin
val uws = UnicastWorkSubject()

generateInts(uws, 1, 15)

// prints lines 1..5
uws.take(5).collect { println(it) }

// prints lines 6..10
uws.take(5).collect { println(it) }

// prints lines 11..15
uws.take(5).collect { println(it) }
```

## concatArrayEager

Launches all at once and emits all items from a source before items of the next are emitted.

For example, given two sources, if the first is slow, the items of the second won't be emitted until the first has
finished emitting its items. This operators allows all sources to generate items in parallel but then still emit those
items in the order their respective `Flow`s are listed.

Note that each source is consumed in an unbounded manner and thus, depending on the speed of
the current source and the collector, the operator may retain items longer and may use more memory
during its execution.

```kotlin
concatArrayEager(
        range(1, 5).onStart { delay(200) },
        range(6, 5)
)
.assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
```

## Flow.concatMapEager

Maps the upstream values into [Flow]s and launches them all at once, then
emits items from a source before items of the next are emitted.

For example, given two inner sources, if the first is slow, the items of the second won't be emitted until the first has
finished emitting its items. This operators allows all sources to generate items in parallel but then still emit those
items in the order their respective `Flow`s are mapped in.

Note that the upstream and each source is consumed in an unbounded manner and thus,
depending on the speed of the current source and the collector, the operator may retain
items longer and may use more memory during its execution.

```kotlin
range(1, 5)
.concatMapEager {
    range(it * 10, 5).onEach { delay(100) }
}
.assertResult(
        10, 11, 12, 13, 14,
        20, 21, 22, 23, 24,
        30, 31, 32, 33, 34,
        40, 41, 42, 43, 44,
        50, 51, 52, 53, 54
)
```

## Flow.amb

Starts collecting all source [Flow]s and relays the items of the first one to emit an item,
cancelling the rest.

```kotlin
amb(
    range(1, 5).onStart { delay(1000) },
    range(6, 5).onStart { delay(100) }
)
.assertResult(6, 7, 8, 9, 10)
```