package com.bazarfx.storage;

import com.bazarfx.model.*;
import com.bazarfx.util.IdGenerator;
import com.bazarfx.util.PasswordHasher;

import java.util.ArrayList;
import java.util.List;

/**
 * Populates a brand-new database with realistic demo content the first time the app is
 * run (whenever the users table comes back empty) - accounts, listings with several
 * photos each, orders sitting at every stage of the pipeline, a couple of reviews and a
 * sample chat thread - so every screen (Dashboard, Orders, Reports, Messages...) has
 * something to show immediately instead of an empty state.
 */
final class DemoDataSeeder {

    private DemoDataSeeder() {}

    static void seed(FileStorageManager storage) {
        String pw = "demo1234";

        User shahnewaj = user(storage, "shahnewaj", "shahnewaj@kuet.ac.bd", "+8801700000000", "Khulna", pw);
        User rima = user(storage, "rima.rahman", "rima@example.com", "+8801811111111", "Dhaka", pw);
        User tanvir = user(storage, "tanvir_k", "tanvir@example.com", "+8801922222222", "Rajshahi", pw);
        User buyerDemo = user(storage, "buyer", "buyer@example.com", "+8801633333333", "Khulna", pw);

        Product camera = product(storage, shahnewaj, "Vintage Canon AE-1 Camera",
                "35mm SLR film camera in mint condition, recently serviced. Comes with the original 50mm f/1.8 lens.",
                Category.ELECTRONICS, 15500, Condition.USED,
                "https://picsum.photos/id/250/640/480", "https://picsum.photos/id/26/640/480", "https://picsum.photos/id/96/640/480");

        Product laptop = product(storage, shahnewaj, "ASUS TUF Gaming Laptop",
                "Ryzen 7, RTX 3060, 16GB RAM, 512GB NVMe SSD. Great condition, includes charger and sleeve.",
                Category.ELECTRONICS, 85000, Condition.USED,
                "https://picsum.photos/id/0/640/480", "https://picsum.photos/id/60/640/480");

        Product desk = product(storage, rima, "Ergonomic Wooden Desk",
                "Solid oak desk, perfect for a home office setup. Minor scuffs on one leg, otherwise flawless.",
                Category.FURNITURE, 6200, Condition.NEW,
                "https://picsum.photos/id/20/640/480", "https://picsum.photos/id/106/640/480", "https://picsum.photos/id/30/640/480");

        Product bike = product(storage, tanvir, "Yamaha FZS Motorcycle",
                "2021 model, 8,200 km, single owner, all papers up to date. New tires fitted last month.",
                Category.VEHICLES, 195000, Condition.USED,
                "https://picsum.photos/id/111/640/480", "https://picsum.photos/id/122/640/480");

        Product jacket = product(storage, rima, "Leather Jacket (Size M)",
                "Genuine leather biker jacket, barely worn. Smoke-free home.",
                Category.FASHION, 4200, Condition.USED,
                "https://picsum.photos/id/219/640/480");

        Product books = product(storage, tanvir, "Competitive Programming Book Bundle",
                "CLRS, CP4, and a used copy of Elements of Programming Interviews. Some highlighting inside.",
                Category.BOOKS, 1800, Condition.USED,
                "https://picsum.photos/id/24/640/480", "https://picsum.photos/id/48/640/480");

        camera.setViewCount(42);
        laptop.setViewCount(110);
        desk.setViewCount(19);
        bike.setViewCount(76);
        jacket.setViewCount(31);
        books.setViewCount(14);
        for (Product p : List.of(camera, laptop, desk, bike, jacket, books)) storage.saveProduct(p);

        // Orders across every stage of the pipeline, so Orders / Dashboard / Reports all have data.
        order(storage, buyerDemo, shahnewaj, OrderStatus.DELIVERED, item(camera, 1));
        order(storage, buyerDemo, rima, OrderStatus.SHIPPED, item(desk, 1));
        order(storage, rima, tanvir, OrderStatus.CONFIRMED, item(bike, 1));
        order(storage, tanvir, shahnewaj, OrderStatus.PENDING, item(laptop, 1));
        Order deliveredForReview = order(storage, buyerDemo, rima, OrderStatus.DELIVERED, item(jacket, 1));

        Review review = new Review(IdGenerator.newId(), deliveredForReview.getId(), buyerDemo.getId(), rima.getId(), 5,
                "Exactly as described, fast reply and easy handover. Would buy again!");
        storage.addReview(review);
        rima.setSellerRatingAvg(5.0);
        rima.setSellerRatingCount(1);
        storage.saveUser(rima);
        shahnewaj.setSellerRatingAvg(4.8);
        shahnewaj.setSellerRatingCount(6);
        storage.saveUser(shahnewaj);

        // Sample chat thread on the camera listing.
        storage.addMessage(new Message(IdGenerator.newId(), camera.getId(), buyerDemo.getId(), shahnewaj.getId(),
                buyerDemo.getId(), "Hi! Is the camera still available? Does the light meter work?"));
        storage.addMessage(new Message(IdGenerator.newId(), camera.getId(), buyerDemo.getId(), shahnewaj.getId(),
                shahnewaj.getId(), "Yes, still available - light meter is spot on, just replaced the battery."));
        storage.addMessage(new Message(IdGenerator.newId(), camera.getId(), buyerDemo.getId(), shahnewaj.getId(),
                buyerDemo.getId(), "Great, I'll take it. Can we meet near Khulna University tomorrow?"));
    }

    private static User user(FileStorageManager storage, String username, String email, String phone, String location, String pw) {
        String salt = PasswordHasher.generateSalt();
        User u = new User(IdGenerator.newId(), username, email, phone, location, PasswordHasher.hash(pw, salt), salt);
        storage.saveUser(u);
        return u;
    }

    private static Product product(FileStorageManager storage, User seller, String title, String description,
                                    Category category, double price, Condition condition, String... photoUrls) {
        Product p = new Product(IdGenerator.newId(), seller.getId(), title, description, category, price, condition);
        List<String> photos = new ArrayList<>(List.of(photoUrls));
        p.setPhotoPaths(photos);
        p.setThumbnailPaths(photos); // remote demo URLs double as their own thumbnails
        storage.saveProduct(p);
        return p;
    }

    private static OrderItem item(Product p, int qty) {
        return new OrderItem(p.getId(), p.getTitle(), p.getPrice(), qty);
    }

    private static Order order(FileStorageManager storage, User buyer, User seller, OrderStatus status, OrderItem... items) {
        List<OrderItem> list = new ArrayList<>(List.of(items));
        double total = list.stream().mapToDouble(OrderItem::lineTotal).sum();
        Order o = new Order(IdGenerator.newId(), buyer.getId(), seller.getId(), list, total);
        o.setStatus(status);
        storage.addOrder(o);
        return o;
    }
}
