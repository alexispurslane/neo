# Basic code style stuff

A lot of this stuff may seem really pedantic but I think it's in the interest
of a) you grokking wtf I'm doing and b) my sanity as a perfectionist and c)
overall code coherence and quality. Much of this you probably already know.

1. Use coroutines `with(Dispatchers.IO)` to deal with any and all tasks that
   may happen at random times (such as dealing with updating the UI based on
   new data or updating data sources based on messages from the server) and
   long-running/async tasks. (This is a basic GUI principle, I'm just saying
   *be aggressive with it*. Kotlin has M:N green threads and cooperative
   scheduling so *use them*, it really improves the experience)
2. Adhere as tightly as possible to the [Android App
   Architecture](https://developer.android.com/topic/architecture) and [its
   recommendations](https://developer.android.com/topic/architecture/recommendations), even if you
   think it will make things more verbose. The entire reason I'm making this
   project is that when a clear architecture with separation of concerns and
   a unidirectional data flow and single source of truth for each thing is not
   respected, things get evil. So here are the layers (data flows from the last
   layer to the first, events flow from the first to the last):
    - [**UI
      Layer**](https://developer.android.com/topic/architecture/ui-layer):
      displays the application's data and the UI state on screen as
      a presentation state, and changes it whenever the data changes (via e.g.
      network events) or state changes (e.g., because of user input events)
        - **View Layer**: defined as a declarative, purely functional
          transformation of the reactive stream of the current user interface
          state, and application data, into a UI presentation.  Receives
          (almost) all data and state from a deeper layer, and passes (almost)
          all events it recieves back down the layer hierarchy.
        - **View Model Layer**: holds the UI state, responds to user events by
          either changing that UI state, or passing the events (with some
          processing) on to a deeper layer, and exposes the UI state, and
          reactive stream of application data (with some processing) to the
          view layer.
    - **Domain Layer**: this one is optional and we don't have it because
      really all of the business logic is contained in the Revolt server
    - [**Data
      Layer**](https://developer.android.com/topic/architecture/data-layer):
      handles talking to any databases, the network, any other devices, and all
      of the business logic, and presents the resultant relevant application
      data as a set of reactive immutable streams to the upper layers
        - **Repositories**: there is one repository responsible for each
          different type of data the application has (think of them like
          different tables in a relational database), which collects that type
          of data from all the relevant sources, resolves conflicts between it,
          does any involved logic on it, and presents it as a single source of
          truth reactive stream to the rest of the application, abstracting
          over read-related business logic and multiple data sources. That
          repository should also contain any business logic regarding updating,
          modifying, or deleting anything related to the data it deals with,
          including choosing which data sources to notify about those things,
          and updating its own reactive stream with those changes if that will
          not automatically happen as a result of propegating the change to the
          data source (so if the data source will not re-broadcast changes so
          it ends up automatically in the stream already). That business logic
          is what will be called by te View Model layer. For example, we have:
            - An emoji repository
            - A message repository
            - A server repository
            - A user repository
        - **Data Sources**: data sources represent wrapper APIs for the
          specific operations you can do with any specific individual data
          sources an application might use (a REST API, a local database, the
          filesystem, etc). For us, that's a REST API that we turn into a nice
          Kotlin library using Retrofit and Jackson. If data sources require initialization
          or maintenence, you can define singleton objects to deal with that
          separate from the data sources but through which the data sources are
          accessed.
4. Even though it's awkward, try to make things as fully-typed as you can. So
   don't be afraid to use Kotlin's abilities to imitate an ML-style ADT if you
   can (tip: for sum types, use an abstract base class with the name of the
   type, and then a bunch of nested data classes that all inherit from that
   abstract base class with the names of the variants and their arguments being
   the product type that comes with that variant. If the variant has no product
   type, use a singleton (`companion`) object).
5. The hoops you have to jump through to get a persistant background service
   are absurd and annoying, and I've got it to only *mostly* work as of yet
   despite stealing the code from the main FLOSS app that purports to do it
   robustly, so don't touch it unless you know what you're doing.
6. I typically don't comment anything. Instead I use long descriptive names
   (which is fine because autocomplete exists). I suggest you do the same. Only
   leave comments when something is confusing, non-intuitive, or not all
   implemented in the place you would logically expect it to be ~~or you have
   an intrustive thought that needs to be externalized as an unhinged
   comment~~. When you leave a comment to explain things, don't worry about
   brevity. These are our notes, so drop a paragraph or two, it's chill.
   **Corellary to the above:** if you have a question about how/why something
   works, read the code, and if that doesn't answer your question, hopefully
   I'll already have marked it as confusing and put a comment there for you. If
   I haven't ask me and I'll put a comment explaining things there for
   posterity. Code comments *are our documentation*
7. Functions length is not a thing you should be worrying about. Just ensure
   that the function is doing one logical task. Try to make sure that someone
   can understand what the function is doing and at a fairly granular level
   *how* it is doing it just by looking at that one function. No Ravioli code
   please. But separate each logical sub-step in any given block with a blank
   line *please*.
8. Just don't use regular classes when you're not required to. The only classes
   should be ViewModels, Applications, Activities, Repositories, and
   DataSources --- things that are logically more modules than classes, since
   they're usually singletons that really just act as state encapsulation and
   namespacing. So basically, try to pretend this is OCaml lol. That's what
   I do! :P

# Key library documentation

- [Jetpack Compose (base UI
  tool)](https://developer.android.com/jetpack/compose/documentation)
- [Material3 (specific UI
  widgets)](https://developer.android.com/jetpack/compose/documentation)
- [Jackson (JSON
  parsing)](https://github.com/FasterXML/jackson-docs?tab=readme-ov-file)
- [Retrofit 2 (HTTP API into Kotlin
  interface)](https://square.github.io/retrofit/)
- [Hilt/Dagger (direct
  injection)](https://developer.android.com/training/dependency-injection)
- [Compose Richtext](https://halilibo.com/compose-richtext/)
- [Compose Navigation library](https://developer.android.com/guide/navigation)
- [AndroidX (modern Android SDK. if you're using something from the plain
  `android` package its so over
  chat)](https://developer.android.com/reference/)

# Useful Kotlin notes

1. Kotlin should be really easy to learn, it's sort of just like, Java except
   made 1000% more concise and with the best asynchrony/threading model I've
   ever seen in my life, with some things from TypeScript like null safety and
   flow analysis for that null safety. If you have any experience with modern
   post 2015-designed languages you should be able to read and write basic
   Kotlin instantly and learn the rest by osmosis and editor completion. That's
   what I did and I didn't have a 10,000 line worked example available.
   However, if you feel the need, this looks good: <https://play.kotlinlang.org/byExample/overview>.
2. Useful domain-relevant links in order of importance:
    - [The built-in Kotlin
      flows](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/)
      and the [Compose-specific Kotlin
      flows](https://developer.android.com/kotlin/flow), as well as the
      [Compose Snapshot
      classes](https://developer.android.com/reference/kotlin/androidx/compose/runtime/snapshots/package-summary).
      Trust me, understanding all the nuanced differences and pros/cons and
      uses cases for these is *crucial* to things working right.
    - [Coroutines](https://kotlinlang.org/docs/coroutines-guide.html). This is
      the other really really big important one. 
    - [Collections](https://kotlinlang.org/docs/collections-overview.html)
    - [Null safety](https://kotlinlang.org/docs/null-safety.html)

# Revolt notes

1. [The Revolt API](https://developers.revolt.chat/api/) and the [Revolt Event
   list](https://developers.revolt.chat/stack/bonfire/events) are your bibles.
   You really won't need anything else.
