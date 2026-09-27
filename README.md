# BazarFX

BazarFX is a desktop marketplace application built with **JavaFX** and backed by a local **SQLite** database. It lets users sign up, browse and search listings, sell items, message other users, place orders, track order status, leave reviews, and view seller reports — all in a single responsive desktop app.

## Features

- **Authentication** — sign up / log in with hashed & salted passwords
- **Browse & Search** — filter listings by category, price range, and condition; sort by newest, price, or popularity
- **Sell an Item** — create listings with photos, category, condition, and price
- **Cart & Checkout** — add items to cart and place orders
- **Order Tracking** — orders move through a status lifecycle over time
- **Messaging** — buyer/seller conversations per listing
- **Reviews** — buyers rate and review completed orders
- **Seller Dashboard & Reports** — sellers manage their listings and view generated sales reports
- **Live Notifications** — in-app toast popups for order updates, new messages, and published listings
- **Live Exchange Rates** — USD conversion rates fetched from a public API and shown in the UI

## Tech Stack

- Java 21, JavaFX 21 (FXML views + CSS styling)
- SQLite (via `sqlite-jdbc`) for persistent storage
- Gson for JSON parsing
- Java `java.net.http.HttpClient` for networking
- Maven (`javafx-maven-plugin`, `maven-shade-plugin`) for build/packaging

## Project Structure

```
src/main/java/com/bazarfx/
  model/          domain classes (Product, User, Order, Review, Message, ...)
  service/        business logic (AuthService, ProductService, OrderService, ...)
  storage/        SQLite persistence layer (FileStorageManager, DemoDataSeeder)
  concurrency/    background workers (thread pools, scheduled tasks, networking)
  notification/   in-app notification hierarchy
  controller/     JavaFX controllers, one per screen
  util/           helpers (validation, hashing, id generation, image utils)
src/main/resources/com/bazarfx/
  view/           FXML layouts, one per screen
  css/            stylesheet
```

## Running the App

```bash
mvn clean javafx:run
```

On launch, `Main` initializes the app context (storage + services), starts the background services (auto-save, live exchange rates, order-status simulation, notification polling), and opens the login screen.

## Where the Core Concepts Live

Just so it's easy to find later, here's roughly where each of the required topics shows up in the codebase:

- **Advanced OOP** — `notification/AppNotification.java` is an abstract class with `OrderStatusNotification`, `NewMessageNotification`, and `ListingPublishedNotification` extending it (inheritance + polymorphism, no type-switching needed to render a popup). `concurrency/ImageProcessor.java` defines a `Callback` interface for async results. Custom exceptions (`ProductService.ValidationException`) and enums (`Category`, `Condition`, `ListingStatus`, `OrderStatus`) are used throughout, and fields are kept private with validated setters via `util/InputValidator.java`.
- **JavaFX UI** — the FXML views under `resources/.../view/` mix `BorderPane` and `StackPane` (`ShellView.fxml`), `SplitPane` (`MessagesView.fxml`), `FlowPane` (`BrowseView.fxml`), plus `PasswordField`, `ComboBox`, `Spinner`, and `ProgressIndicator` across the login, sell-listing, product-detail, and reports screens.
- **Responsive layout** — `SceneRouter.bindResponsiveScale()` rescales the root font size against window width/height (with `style.css` written in `em` units), so the whole UI scales smoothly on resize; `HBox`/`VBox` grow constraints and `imageView` bindings in `BrowseController`/`ProductDetailController` keep images and bars fluid too.
- **Concurrency** — the whole `concurrency/` package: a fixed thread pool in `ImageProcessor` for background thumbnail generation, `ScheduledExecutorService`s in `ExchangeRateFetcher`, `OrderStatusSimulator`, and `AutoSaveService`, plus dedicated background `Thread`s in `NotificationPoller` and `SearchIndex` — all kept off the JavaFX Application Thread and synced back via `Platform.runLater()`.
- **Database** — `storage/FileStorageManager.java` opens a SQLite connection and creates 7 related tables (`users`, `products`, `product_images`, `orders`, `order_items`, `reviews`, `messages`) with `FOREIGN KEY`/`ON DELETE CASCADE` relationships, all reads/writes going through `PreparedStatement`.
- **CRUD** — cleanest in `service/ProductService.java`: `createListing`, `getById`/`listingsBySeller`/`allActive`, `updateListing`/`setStatus`, `deleteListing` — the same pattern repeats in `AuthService`, `OrderService`, `ReviewService`, and `MessagingService`.
- **Networking & JSON** — `concurrency/ExchangeRateFetcher.java` hits a live exchange-rate API with `HttpClient`, parses the JSON body with Gson, and falls back gracefully if the request fails — all on a background scheduled thread.
