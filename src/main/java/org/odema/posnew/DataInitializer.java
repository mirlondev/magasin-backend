package org.odema.posnew;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.odema.posnew.entity.*;
import org.odema.posnew.entity.enums.*;
import org.odema.posnew.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
//@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShiftReportRepository shiftReportRepository;
    private final RefundRepository refundRepository;
    private final PasswordEncoder passwordEncoder;

    private final Random random = new Random();

    @Override
    @Transactional
    public void run(String @NonNull ... args) throws Exception {
        if (userRepository.count() > 0) {
            log.info("Data already initialized. Skipping...");
            return;
        }

        log.info("Starting data initialization...");

        // 1. Create Users
        List<User> users = createUsers();
        log.info("Created {} users", users.size());

        // 2. Create Stores
        List<Store> stores = createStores(users);
        log.info("Created {} stores", stores.size());

        // 3. Assign stores to users
        assignStoresToUsers(users, stores);

        // 4. Create Categories
        List<Category> categories = createCategories();
        log.info("Created {} categories", categories.size());

        // 5. Create Products
        List<Product> products = createProducts(categories);
        log.info("Created {} products", products.size());

        // 6. Create Inventory
        createInventory(stores, products);
        log.info("Created inventory for all stores");

        // 7. Create Customers
        List<Customer> customers = createCustomers();
        log.info("Created {} customers", customers.size());

        // 8. Create Shift Reports
        List<ShiftReport> shifts = createShiftReports(stores, users);
        log.info("Created {} shift reports", shifts.size());

        // 9. Create Orders
        createOrders(stores, users, customers, products, shifts);
        log.info("Created sample orders");

        log.info("Data initialization completed successfully!");
        printLoginCredentials();
    }

    private List<User> createUsers() {
        List<User> users = new ArrayList<>();

        // Admin users
        users.add(User.builder()
                .username("admin")
                .email("admin@boutique.com")
                .password(passwordEncoder.encode("admin123"))
                .phone("+242 06 123 4567")
                .address("123 Avenue de l'Indépendance")
                .active(true)
                .userRole(UserRole.ADMIN)
                .lastLogin(LocalDateTime.now().minusDays(1))
                .build());

        users.add(User.builder()
                .username("admin2")
                .email("admin2@boutique.com")
                .password(passwordEncoder.encode("admin123"))
                .phone("+242 06 234 5678")
                .address("45 Boulevard Denis Sassou")
                .active(true)
                .userRole(UserRole.ADMIN)
                .lastLogin(LocalDateTime.now().minusHours(5))
                .build());

        // Store managers
        users.add(User.builder()
                .username("manager_centre")
                .email("manager.centre@boutique.com")
                .password(passwordEncoder.encode("manager123"))
                .phone("+242 06 345 6789")
                .address("78 Rue Mbochi")
                .active(true)
                .userRole(UserRole.STORE_ADMIN)
                .lastLogin(LocalDateTime.now().minusHours(2))
                .build());

        users.add(User.builder()
                .username("manager_poto")
                .email("manager.poto@boutique.com")
                .password(passwordEncoder.encode("manager123"))
                .phone("+242 06 456 7890")
                .address("22 Avenue Foch")
                .active(true)
                .userRole(UserRole.STORE_ADMIN)
                .lastLogin(LocalDateTime.now().minusHours(3))
                .build());

        users.add(User.builder()
                .username("manager_tietie")
                .email("manager.tietie@boutique.com")
                .password(passwordEncoder.encode("manager123"))
                .phone("+242 06 555 6789")
                .address("15 Rue Vindza")
                .active(true)
                .userRole(UserRole.STORE_ADMIN)
                .lastLogin(LocalDateTime.now().minusHours(4))
                .build());

        // Cashiers
        users.add(User.builder()
                .username("caissier1")
                .email("caissier1@boutique.com")
                .password(passwordEncoder.encode("cashier123"))
                .phone("+242 06 567 8901")
                .address("56 Rue Vindza")
                .active(true)
                .userRole(UserRole.CASHIER)
                .lastLogin(LocalDateTime.now())
                .build());

        users.add(User.builder()
                .username("caissier2")
                .email("caissier2@boutique.com")
                .password(passwordEncoder.encode("cashier123"))
                .phone("+242 06 678 9012")
                .address("89 Avenue Marien Ngouabi")
                .active(true)
                .userRole(UserRole.CASHIER)
                .lastLogin(LocalDateTime.now().minusMinutes(30))
                .build());

        users.add(User.builder()
                .username("caissier3")
                .email("caissier3@boutique.com")
                .password(passwordEncoder.encode("cashier123"))
                .phone("+242 06 789 0123")
                .address("34 Rue Loutassi")
                .active(true)
                .userRole(UserRole.CASHIER)
                .lastLogin(LocalDateTime.now().minusHours(1))
                .build());

        return userRepository.saveAll(users);
    }

    private List<Store> createStores(List<User> users) {
        List<Store> stores = new ArrayList<>();

        stores.add(Store.builder()
                .name("Boutique Centre-Ville")
                .address("Avenue Charles de Gaulle")
                .city("Pointe-Noire")
                .postalCode("00242")
                .country("Congo")
                .phone("+242 22 294 5001")
                .email("centre@boutique.com")
                .latitude(new BigDecimal("-4.7827"))
                .longitude(new BigDecimal("11.8544"))
                .openingHours("Lun-Sam: 08:00-20:00, Dim: 09:00-18:00")
                .storeType(StoreType.MAIN)
                .status(StoreStatus.ACTIVE)
                .isActive(true)
                .storeAdmin(users.get(2)) // manager_centre
                .build());

        stores.add(Store.builder()
                .name("Boutique Poto-Poto")
                .address("Marché Poto-Poto")
                .city("Pointe-Noire")
                .postalCode("00242")
                .country("Congo")
                .phone("+242 22 294 5002")
                .email("potopoto@boutique.com")
                .latitude(new BigDecimal("-4.7689"))
                .longitude(new BigDecimal("11.8632"))
                .openingHours("Lun-Sam: 07:30-19:30, Dim: 08:00-17:00")
                .storeType(StoreType.BRANCH)
                .status(StoreStatus.ACTIVE)
                .isActive(true)
                .storeAdmin(users.get(3)) // manager_poto
                .build());

        stores.add(Store.builder()
                .name("Boutique Tié-Tié")
                .address("Quartier Tié-Tié")
                .city("Pointe-Noire")
                .postalCode("00242")
                .country("Congo")
                .phone("+242 22 294 5003")
                .email("tietie@boutique.com")
                .latitude(new BigDecimal("-4.7945"))
                .longitude(new BigDecimal("11.8456"))
                .openingHours("Lun-Sam: 08:00-19:00")
                .storeType(StoreType.BRANCH)
                .status(StoreStatus.ACTIVE)
                .isActive(true)
                .storeAdmin(users.get(4)) // manager_tietie
                .build());

        return storeRepository.saveAll(stores);
    }

    private void assignStoresToUsers(List<User> users, List<Store> stores) {
        // Assign cashiers to stores
        users.get(5).setAssignedStore(stores.get(0)); // caissier1 -> Centre-Ville
        users.get(6).setAssignedStore(stores.get(1)); // caissier2 -> Poto-Poto
        users.get(7).setAssignedStore(stores.get(2)); // caissier3 -> Tié-Tié

        userRepository.saveAll(users);
    }

    private List<Category> createCategories() {
        List<Category> categories = new ArrayList<>();

        // Main categories
        Category alimentaire = Category.builder()
                .name("Alimentaire")
                .description("Produits alimentaires et boissons")
                .imageUrl("/images/categories/alimentaire.jpg")
                .isActive(true)
                .build();
        categories.add(alimentaire);

        Category hygiene = Category.builder()
                .name("Hygiène & Beauté")
                .description("Produits d'hygiène et de beauté")
                .imageUrl("/images/categories/hygiene.jpg")
                .isActive(true)
                .build();
        categories.add(hygiene);

        Category maison = Category.builder()
                .name("Maison & Entretien")
                .description("Produits pour la maison et l'entretien")
                .imageUrl("/images/categories/maison.jpg")
                .isActive(true)
                .build();
        categories.add(maison);

        Category electronique = Category.builder()
                .name("Électronique")
                .description("Appareils et accessoires électroniques")
                .imageUrl("/images/categories/electronique.jpg")
                .isActive(true)
                .build();
        categories.add(electronique);

        categoryRepository.saveAll(categories);

        // Sub-categories for Alimentaire
        List<Category> subCatAlimentaire = new ArrayList<>();

        subCatAlimentaire.add(Category.builder()
                .name("Boissons")
                .description("Boissons gazeuses, jus, eau")
                .parentCategory(alimentaire)
                .isActive(true)
                .build());

        subCatAlimentaire.add(Category.builder()
                .name("Produits laitiers")
                .description("Lait, yaourt, fromage")
                .parentCategory(alimentaire)
                .isActive(true)
                .build());

        subCatAlimentaire.add(Category.builder()
                .name("Épicerie")
                .description("Riz, pâtes, conserves")
                .parentCategory(alimentaire)
                .isActive(true)
                .build());

        categoryRepository.saveAll(subCatAlimentaire);
        alimentaire.setSubCategories(subCatAlimentaire);

        // Sub-categories for Hygiène
        List<Category> subCatHygiene = new ArrayList<>();

        subCatHygiene.add(Category.builder()
                .name("Soins corporels")
                .description("Savons, gels douche, déodorants")
                .parentCategory(hygiene)
                .isActive(true)
                .build());

        subCatHygiene.add(Category.builder()
                .name("Soins capillaires")
                .description("Shampoings, après-shampoings")
                .parentCategory(hygiene)
                .isActive(true)
                .build());

        categoryRepository.saveAll(subCatHygiene);
        hygiene.setSubCategories(subCatHygiene);

        return categories;
    }

    private List<Product> createProducts(List<Category> categories) {
        List<Product> products = new ArrayList<>();

        // Get sub-categories
        Category boissons = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Boissons"))
                .findFirst().orElse(categories.get(0));

        Category laitiers = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Produits laitiers"))
                .findFirst().orElse(categories.get(0));

        Category epicerie = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Épicerie"))
                .findFirst().orElse(categories.get(0));

        Category soinsCorps = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Soins corporels"))
                .findFirst().orElse(categories.get(1));

        Category soinsCheveux = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Soins capillaires"))
                .findFirst().orElse(categories.get(1));

        // Boissons
        products.add(createProduct("Coca-Cola 1.5L", "SKU-COCA-1500", "3664700010051",
                "Boisson gazeuse Coca-Cola 1.5 litres", new BigDecimal("1500"), boissons));

        products.add(createProduct("Eau Minérale Ngonguié 1.5L", "SKU-NGON-1500", "3700000100011",
                "Eau minérale naturelle du Congo", new BigDecimal("500"), boissons));

        products.add(createProduct("Jus d'Orange Tropicana 1L", "SKU-TROP-1000", "3664700020052",
                "Jus d'orange 100% pur jus", new BigDecimal("2000"), boissons));

        products.add(createProduct("Sprite 33cl", "SKU-SPRI-330", "3664700010068",
                "Boisson gazeuse citron-citron vert", new BigDecimal("600"), boissons));

        products.add(createProduct("Fanta Orange 33cl", "SKU-FANT-330", "3664700010075",
                "Boisson gazeuse saveur orange", new BigDecimal("600"), boissons));

        // Produits laitiers
        products.add(createProduct("Lait Nido 400g", "SKU-NIDO-400", "7613035258914",
                "Lait en poudre enrichi", new BigDecimal("3500"), laitiers));

        products.add(createProduct("Yaourt Danone Nature x4", "SKU-DANO-NAT4", "3033710071487",
                "Pack de 4 yaourts nature", new BigDecimal("2000"), laitiers));

        products.add(createProduct("Lait Gloria 1L", "SKU-GLOR-1000", "8711200247578",
                "Lait UHT demi-écrémé", new BigDecimal("1200"), laitiers));

        // Épicerie
        products.add(createProduct("Riz Parfumé Uncle Ben's 1kg", "SKU-RICE-1000", "5410001054014",
                "Riz long grain parfumé", new BigDecimal("3000"), epicerie));

        products.add(createProduct("Huile Végétale Lesieur 1L", "SKU-OIL-1000", "3228857000117",
                "Huile végétale de qualité", new BigDecimal("2500"), epicerie));

        products.add(createProduct("Spaghetti Panzani 500g", "SKU-PAST-500", "3083680085014",
                "Pâtes italiennes", new BigDecimal("1500"), epicerie));

        products.add(createProduct("Tomate Concentrée 70g", "SKU-TOM-070", "3083680003018",
                "Concentré de tomate", new BigDecimal("500"), epicerie));

        products.add(createProduct("Sucre Cristallisé 1kg", "SKU-SUG-1000", "3560070513895",
                "Sucre blanc cristallisé", new BigDecimal("1800"), epicerie));

        products.add(createProduct("Sel Iodé 500g", "SKU-SALT-500", "3560070514014",
                "Sel de cuisine iodé", new BigDecimal("400"), epicerie));

        products.add(createProduct("Cube Maggi x24", "SKU-MAG-24", "7613035449527",
                "Cubes d'assaisonnement", new BigDecimal("1200"), epicerie));

        // Soins corporels
        products.add(createProduct("Savon Lux 90g", "SKU-LUX-90", "8901030775178",
                "Savon de toilette parfumé", new BigDecimal("800"), soinsCorps));

        products.add(createProduct("Gel Douche Palmolive 250ml", "SKU-PALM-250", "8714789732534",
                "Gel douche hydratant", new BigDecimal("2500"), soinsCorps));

        products.add(createProduct("Déodorant Nivea Roll-On", "SKU-NIV-DEO", "4005808748228",
                "Déodorant bille 48h", new BigDecimal("3000"), soinsCorps));

        products.add(createProduct("Dentifrice Signal 75ml", "SKU-SIG-75", "8717163728147",
                "Protection complète", new BigDecimal("1500"), soinsCorps));

        // Soins capillaires
        products.add(createProduct("Shampoing Pantene 200ml", "SKU-PAN-200", "8001090898289",
                "Shampoing réparateur", new BigDecimal("3500"), soinsCheveux));

        products.add(createProduct("Huile Capillaire Olive 125ml", "SKU-OLV-125", "3700000200012",
                "Huile d'olive pour cheveux", new BigDecimal("2000"), soinsCheveux));

        // Maison & Entretien
        products.add(createProduct("Javel Eau de Vie 1L", "SKU-JAV-1000", "3700000300013",
                "Eau de javel désinfectante", new BigDecimal("1000"), categories.get(2)));

        products.add(createProduct("Savon de Marseille 300g", "SKU-MAR-300", "3256220017008",
                "Savon pur pour lessive", new BigDecimal("1500"), categories.get(2)));

        products.add(createProduct("Éponge Scotch-Brite x3", "SKU-SCO-3", "4046719337583",
                "Pack de 3 éponges", new BigDecimal("1800"), categories.get(2)));

        // Électronique
        products.add(createProduct("Chargeur USB Universel", "SKU-CHG-USB", "6934177708633",
                "Chargeur 2A compatible smartphones", new BigDecimal("5000"), categories.get(3)));

        products.add(createProduct("Écouteurs Filaires", "SKU-EAR-001", "6934177708640",
                "Écouteurs jack 3.5mm", new BigDecimal("3000"), categories.get(3)));

        products.add(createProduct("Câble USB Type-C 1m", "SKU-CAB-TC1", "6934177708657",
                "Câble de charge rapide", new BigDecimal("2500"), categories.get(3)));

        return productRepository.saveAll(products);
    }

    private Product createProduct(String name, String sku, String barcode, String description,
                                  BigDecimal price, Category category) {
        return Product.builder()
                .name(name)
                .sku(sku)
                .barcode(barcode)
                .description(description)
                .price(price)
                .quantity(0) // Initial quantity - actual stock tracked in Inventory
                .category(category)
                .imageUrl("/images/products/" + sku.toLowerCase() + ".jpg")
                .build();
    }

    private void createInventory(List<Store> stores, List<Product> products) {
        List<Inventory> inventories = new ArrayList<>();

        for (Store store : stores) {
            for (Product product : products) {
                int baseQuantity = random.nextInt(50) + 20; // 20-70 units

                // Adjust quantity based on store type
                int quantity = store.getStoreType() == StoreType.MAIN ?
                        baseQuantity + 30 : baseQuantity;

                BigDecimal costMultiplier = new BigDecimal("0.70"); // 30% margin
                BigDecimal unitCost = product.getPrice().multiply(costMultiplier);

                inventories.add(Inventory.builder()
                        .product(product)
                        .store(store)
                        .quantity(quantity)
                        .unitCost(unitCost)
                        .sellingPrice(product.getPrice())
                        .minStock(10)
                        .maxStock(100)
                        .reorderPoint(15)
                        .stockStatus(determineStockStatus(quantity))
                        .lastRestocked(LocalDateTime.now().minusDays(random.nextInt(30)))
                        .nextRestockDate(LocalDateTime.now().plusDays(random.nextInt(15) + 5))
                        .isActive(true)
                        .notes("Stock initial")
                        .build());
            }
        }

        inventoryRepository.saveAll(inventories);
    }

    private StockStatus determineStockStatus(int quantity) {
        if (quantity == 0) return StockStatus.OUT_OF_STOCK;
        if (quantity <= 15) return StockStatus.LOW_STOCK;
        if (quantity >= 90) return StockStatus.OVER_STOCK;
        return StockStatus.IN_STOCK;
    }

    private List<Customer> createCustomers() {
        List<Customer> customers = new ArrayList<>();

        String[] firstNames = {"Jean", "Marie", "Paul", "Grace", "Michel", "Christine",
                "Joseph", "Ange", "David", "Sarah", "Emmanuel", "Ruth",
                "Daniel", "Esther", "Samuel", "Rebecca", "André", "Joséphine"};

        String[] lastNames = {"Moukoko", "Ngoma", "Okemba", "Samba", "Makaya", "Loubaki",
                "Ondongo", "Mabiala", "Kimbembe", "Nkounkou", "Mouanda", "Tchikaya"};

        String[] streets = {"Avenue de l'Indépendance", "Rue Mbochi", "Boulevard Denis Sassou",
                "Avenue Foch", "Rue Vindza", "Avenue Marien Ngouabi"};

        for (int i = 0; i <= 18; i++) {
            String firstName = firstNames[random.nextInt(firstNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];

            customers.add(Customer.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@email.com")
                    .phone("+242 06 " + String.format("%03d", random.nextInt(1000)) + " " +
                            String.format("%04d", random.nextInt(10000)))
                    .address((random.nextInt(200) + 1) + " " + streets[random.nextInt(streets.length)])
                    .city("Pointe-Noire")
                    .postalCode("00242")
                    .country("Congo")
                    .isActive(true)
                    .loyaltyPoints(random.nextInt(500))
                    .totalPurchases((double) (random.nextInt(50000) + 10000))
                    .lastPurchaseDate(LocalDateTime.now().minusDays(random.nextInt(60)))
                    .build());
        }

        return customerRepository.saveAll(customers);
    }

    private List<ShiftReport> createShiftReports(List<Store> stores, List<User> users) {
        List<ShiftReport> shifts = new ArrayList<>();
        int shiftCounter = 1;

        // Create shifts for the last 7 days
        for (int day = 7; day >= 1; day--) {
            LocalDateTime date = LocalDateTime.now().minusDays(day);

            for (Store store : stores) {
                // Morning shift
                User cashier = users.stream()
                        .filter(u -> u.getUserRole() == UserRole.CASHIER &&
                                (u.getAssignedStore() == null || u.getAssignedStore().equals(store)))
                        .findFirst()
                        .orElse(users.get(5));

                BigDecimal openingBalance = new BigDecimal("50000");
                BigDecimal totalSales = new BigDecimal(random.nextInt(100000) + 50000);
                BigDecimal expectedBalance = openingBalance.add(totalSales);
                BigDecimal actualBalance = expectedBalance.add(new BigDecimal(random.nextInt(2000) - 1000));

                shifts.add(ShiftReport.builder()
                        .shiftNumber("SHIFT-" + String.format("%05d", shiftCounter++))
                        .cashier(cashier)
                        .store(store)
                        .startTime(date.withHour(8).withMinute(0))
                        .endTime(date.withHour(16).withMinute(0))
                        .openingBalance(openingBalance)
                        .closingBalance(actualBalance)
                        .totalSales(totalSales)
                        .totalRefunds(BigDecimal.ZERO)
                        .netSales(totalSales)
                        .expectedBalance(expectedBalance)
                        .actualBalance(actualBalance)
                        .discrepancy(actualBalance.subtract(expectedBalance))
                        .totalTransactions(random.nextInt(50) + 20)
                        .status(ShiftStatus.CLOSED)
                        .notes("Shift normal")
                        .build());
            }
        }

        // Create one open shift for today
        for (Store store : stores) {
            User cashier = users.stream()
                    .filter(u -> u.getUserRole() == UserRole.CASHIER &&
                            (u.getAssignedStore() == null || u.getAssignedStore().equals(store)))
                    .findFirst()
                    .orElse(users.get(5));

            BigDecimal openingBalance = new BigDecimal("50000");

            shifts.add(ShiftReport.builder()
                    .shiftNumber("SHIFT-" + String.format("%05d", shiftCounter++))
                    .cashier(cashier)
                    .store(store)
                    .startTime(LocalDateTime.now().withHour(8).withMinute(0))
                    .openingBalance(openingBalance)
                    .closingBalance(BigDecimal.ZERO) // Will be set when shift closes
                    .totalSales(BigDecimal.ZERO)
                    .totalRefunds(BigDecimal.ZERO)
                    .netSales(BigDecimal.ZERO)
                    .expectedBalance(openingBalance) // Expected = opening balance for new shift
                    .actualBalance(openingBalance) // Actual = opening balance for now
                    .discrepancy(BigDecimal.ZERO) // No discrepancy yet
                    .totalTransactions(0)
                    .status(ShiftStatus.OPEN)
                    .notes("Shift en cours")
                    .build());
        }

        return shiftReportRepository.saveAll(shifts);
    }

    private void createOrders(List<Store> stores, List<User> users,
                              List<Customer> customers, List<Product> products,
                              List<ShiftReport> shifts) {
        int orderCounter = 1;

        // Create orders for the last 7 days
        for (int day = 7; day >= 1; day--) {
            LocalDateTime date = LocalDateTime.now().minusDays(day);
            int ordersPerDay = random.nextInt(15) + 3; // 10-25 orders per day

            for (int i = 0; i < ordersPerDay; i++) {
                Store store = stores.get(random.nextInt(stores.size()));
                Customer customer = random.nextBoolean() ?
                        customers.get(random.nextInt(customers.size())) : null;

                User cashier = users.stream()
                        .filter(u -> u.getUserRole() == UserRole.CASHIER)
                        .skip(random.nextInt(3))
                        .findFirst()
                        .orElse(users.get(5));

                ShiftReport shift = shifts.stream()
                        .filter(s -> s.getStore().equals(store) &&
                                s.getStartTime().toLocalDate().equals(date.toLocalDate()) &&
                                s.getStatus() == ShiftStatus.CLOSED)
                        .findFirst()
                        .orElse(null);

                // Create order with items and totals calculated
                createCompleteOrder(orderCounter++, store, customer, cashier,
                        products, date, shift);
            }
        }

        // Create some refunds
        createRefunds(orderRepository.findAll(), users);
    }

    private void createCompleteOrder(int orderNumber, Store store, Customer customer,
                                     User cashier, List<Product> products,
                                     LocalDateTime date, ShiftReport shift) {
        BigDecimal taxRate = new BigDecimal("0.18"); // 18% TVA
        PaymentMethod[] methods = PaymentMethod.values();
        PaymentMethod paymentMethod = methods[random.nextInt(methods.length)];

        // Create order items first
        List<OrderItem> items = new ArrayList<>();
        int itemCount = random.nextInt(5) + 1; // 1-5 items per order
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (int i = 0; i < itemCount; i++) {
            Product product = products.get(random.nextInt(products.size()));
            int quantity = random.nextInt(3) + 1; // 1-3 units

            BigDecimal unitPrice = product.getPrice();
            BigDecimal totalPrice = unitPrice.multiply(new BigDecimal(quantity));

            // Random discount 0-10%
            BigDecimal discountPercentage = random.nextBoolean() ?
                    BigDecimal.ZERO : new BigDecimal(random.nextInt(11));
            BigDecimal discountAmount = totalPrice.multiply(discountPercentage)
                    .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
            BigDecimal finalPrice = totalPrice.subtract(discountAmount);

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .totalPrice(totalPrice)
                    .discountPercentage(discountPercentage)
                    .discountAmount(discountAmount)
                    .finalPrice(finalPrice)
                    .build();

            items.add(item);
            subtotal = subtotal.add(finalPrice);
            totalDiscount = totalDiscount.add(discountAmount);
        }

        // Calculate order totals
        BigDecimal taxAmount = subtotal.multiply(taxRate);
        BigDecimal totalAmount = subtotal.add(taxAmount);

        // Calculate payment and change
        BigDecimal amountPaid = totalAmount;
        BigDecimal changeAmount = BigDecimal.ZERO;

        if (paymentMethod == PaymentMethod.CASH) {
            // Round up payment to nearest 1000
            amountPaid = totalAmount.divide(new BigDecimal("1000"), 0, BigDecimal.ROUND_UP)
                    .multiply(new BigDecimal("1000"));
            changeAmount = amountPaid.subtract(totalAmount);
        }

        // Create the complete order with all calculated values
        Order order = Order.builder()
                .orderNumber("ORD-" + String.format("%06d", orderNumber))
                .store(store)
                .customer(customer)
                .cashier(cashier)
                .status(OrderStatus.COMPLETED)
                .paymentStatus(PaymentStatus.PAID)
                .paymentMethod(paymentMethod)
                .isTaxable(true)
                .taxRate(taxRate)
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .discountAmount(totalDiscount)
                .totalAmount(totalAmount)
                .amountPaid(amountPaid)
                .changeAmount(changeAmount)
                .createdAt(date.plusHours(random.nextInt(10) + 8))
                .completedAt(date.plusHours(random.nextInt(10) + 8).plusMinutes(random.nextInt(15)))
                .notes(random.nextBoolean() ? "Vente normale" : null)
                .build();

        // Save order first
        order = orderRepository.save(order);

        // Set order reference in items and save them
        for (OrderItem item : items) {
            item.setOrder(order);
        }
        orderItemRepository.saveAll(items);

        // Update order with items list
        order.setItems(items);
    }

    private void createRefunds(List<Order> orders, List<User> users) {
        List<Refund> refunds = new ArrayList<>();
        int refundCounter = 1;

        // Create refunds for ~5% of orders
        int refundCount = Math.max(1, orders.size() / 20);

        for (int i = 0; i < refundCount; i++) {
            Order order = orders.get(random.nextInt(orders.size()));

            if (order.getRefunds() == null || order.getRefunds().isEmpty()) {
                User cashier = users.stream()
                        .filter(u -> u.getUserRole() == UserRole.CASHIER ||
                                u.getUserRole() == UserRole.STORE_ADMIN)
                        .skip(random.nextInt(5))
                        .findFirst()
                        .orElse(order.getCashier());

                BigDecimal refundAmount = order.getTotalAmount()
                        .multiply(new BigDecimal(random.nextBoolean() ? "1.00" : "0.50"));

                String[] reasons = {
                        "Produit défectueux",
                        "Erreur de caisse",
                        "Client non satisfait",
                        "Produit périmé",
                        "Demande du client"
                };

                refunds.add(Refund.builder()
                        .refundNumber("REF-" + String.format("%05d", refundCounter++))
                        .order(order)
                        .store(order.getStore())
                        .cashier(cashier)
                        .refundAmount(refundAmount)
                        .refundType(random.nextBoolean() ? RefundType.FULL : RefundType.PARTIAL)
                        .status(RefundStatus.COMPLETED)
                        .reason(reasons[random.nextInt(reasons.length)])
                        .notes("Remboursement approuvé")
                        .processedAt(order.getCompletedAt().plusHours(random.nextInt(24)))
                        .completedAt(order.getCompletedAt().plusHours(random.nextInt(24) + 1))
                        .isActive(true)
                        .build());
            }
        }

        refundRepository.saveAll(refunds);
    }

    private void printLoginCredentials() {
        log.info("\n" + "=".repeat(60));
        log.info("LOGIN CREDENTIALS");
        log.info("=".repeat(60));
        log.info("ADMIN:");
        log.info("  Username: admin / Password: admin123");
        log.info("  Username: admin2 / Password: admin123");
        log.info("\nSTORE MANAGERS:");
        log.info("  Username: manager_centre / Password: manager123");
        log.info("  Username: manager_poto / Password: manager123");
        log.info("  Username: manager_tietie / Password: manager123");
        log.info("\nCASHIERS:");
        log.info("  Username: caissier1 / Password: cashier123");
        log.info("  Username: caissier2 / Password: cashier123");
        log.info("  Username: caissier3 / Password: cashier123");
        log.info("=".repeat(60) + "\n");
    }
}