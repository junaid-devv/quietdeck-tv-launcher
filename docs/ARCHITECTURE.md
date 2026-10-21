# Architecture

QuietDeck uses native Android framework widgets and Java. There are no third-party runtime libraries, network clients, services, scheduled jobs or requested Android permissions. AndroidX and JUnit exist only in test APKs, not the release app.

## Ownership

| Component | Responsibility |
| --- | --- |
| MainActivity | Native screen, dialogs, lifecycle, user actions and standard system intents |
| AppRepository | Background metadata discovery, package-event refresh and coalesced queries |
| AppEntry | Immutable app metadata and explicit launch component |
| AppGridAdapter | Recycled native app tiles and accessibility descriptions |
| ArtworkLoader | Visible-cell artwork, one worker, bounded queue, byte-bounded LRU and stale-view checks |
| AspectFit | Tested proportional letterbox geometry |
| PreferencesStore / ConfigRules | Legacy preferences, collections, local recents and transactional validated JSON import |
| DeckBackground | Preconstructed paths/shaders for original static vector scenery |
| WallpaperBackground | Center-cropped display of an optional sampled image |

## Discovery and launching

Discovery queries Leanback launchers first, then ordinary launcher activities, deduplicating by package. It excludes Home apps, Settings and known factory/service shortcuts. Labels are locale-sorted. Package changes invalidate metadata and artwork. Returning from another app reuses metadata and cached artwork; it updates the displayed recent list without rediscovering installed packages. A failed refresh keeps the last successful result and offers retry through Settings.

Launches use an explicit installed component. A disappearing or failing activity produces a message and refresh rather than crashing Home. Hidden apps are omitted from Home, collections, recents and search. Search covers all visible installed apps even when started on Home.

## Memory and storage

- Artwork cache: 2 MiB when ActivityManager reports a low-RAM device; 4 MiB otherwise. These are cache budgets, **not total app RAM claims**.
- Raster targets: 240×135 or 320×180 ARGB. Source aspect ratio is preserved; non-banner icons are centered with padding. ImageView uses FIT_CENTER for final display.
- One artwork worker; at most 24 queued requests, with stale requests discarded. Weak targets and package tags prevent recycled tiles receiving the wrong icon.
- No disk artwork cache or bundled content catalogue. Native GridView recycles visible cells.
- Built-in scenery has no full-screen bitmap, draw-time object creation or animations. Clock callbacks run once per minute while resumed.
- Optional wallpaper is sampled to a maximum dimension of 960 pixels in low-RAM mode or 1280 otherwise. A generation token discards stale work. UI-hidden trim drops its reference; resume can reload it.
- Workers and callbacks are shut down when the Activity is destroyed. Trimming evicts artwork without recycling bitmaps still displayed by native views.
- R8 and resource shrinking apply to Gradle release builds. The SDK-only fallback compiles without R8 and is not the distribution build.

## Persistence and privacy

Preferences retain the original private `still` store, pipe-separated favourites and hidden-app set. Recent activity records only up to 12 package identifiers opened through QuietDeck. Collections are local JSON. Standard Android automatic backup and device transfer are excluded.

User-initiated Storage Access Framework exports contain schema 2, favourites, hidden apps, collections and appearance. Imports are limited to 64 KiB and validate package identifiers, group names/counts and option ranges before one preferences transaction. They cannot include commands or launch arbitrary URI intents. Recent activity and wallpaper are excluded. A wallpaper uses a user-selected persisted read grant, released when replaced or removed; no broad storage permission is requested.

The release contains no native `.so` libraries. CI additionally exercises a 16 KiB-page Android runtime. Recheck the final bundle and dependencies after every SDK change.

## Interface and limitations

Native controls provide per-control accessibility semantics, visible focus and normal Android dialogs. App labels use scalable text and ellipsis; the UI follows locale layout direction. Strings are centralized for future localization. Full TalkBack, RTL-language, 4K and physical remote qualification remain explicit device-test gates, not claims inferred from native widgets.

Default Home uses Android's Home role/picker where available, then the manufacturer's settings route. The app does not disable system packages. Back clears search, returns to Home, then exits an ordinary launch; when QuietDeck is the actual default Home it stays on Home. Optional Inputs and document intents fail with an explanation if unsupported.
